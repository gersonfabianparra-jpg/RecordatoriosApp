#!/usr/bin/env python3
"""Servicio remoto simulado para RecordatoriosApp (Actividad 3).

Solo usa la biblioteca estándar de Python 3. Guarda los datos en datos.json.

    python3 servidor.py               # escucha en 0.0.0.0:8080
    python3 servidor.py --puerto 9000

Desde el emulador de Android la computadora se ve como http://10.0.2.2:8080/

Contrato:
    GET    /recordatorios          200 [RecordatorioDto]
    PUT    /recordatorios/{id}     201 creado | 200 reemplazado | 422 datos inválidos
    DELETE /recordatorios/{id}     204 eliminado | 404 no existe
    GET    /salud                  200 {"ok": true}

Administración (solo para pruebas):
    GET  /admin/estado             contadores y modo actual
    POST /admin/modo               {"modo": "normal" | "error500" | "lento" | "rechazar"}
        error500 -> todas las peticiones responden 500
        lento    -> espera 20 s antes de responder (la app corta por timeout)
        rechazar -> los PUT responden 422
    POST /admin/reiniciar          vuelve a los datos iniciales
"""
import argparse
import json
import pathlib
import re
import threading
import time
import uuid
from datetime import datetime, timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ARCHIVO = pathlib.Path(__file__).with_name("datos.json")
PRIORIDADES = {"ALTA", "MEDIA", "BAJA"}
ESTADOS = {"PENDIENTE", "COMPLETADO", "POSPUESTO", "VENCIDO"}
CATEGORIAS = {1, 2, 3, 4, 5}
RUTA_ITEM = re.compile(r"^/recordatorios/([A-Za-z0-9-]{1,64})$")

bloqueo = threading.Lock()
modo = "normal"
contadores = {"GET": 0, "PUT_creados": 0, "PUT_reemplazados": 0, "PUT_rechazados": 0, "DELETE": 0}


def iso(dt):
    return dt.replace(microsecond=0).isoformat()


def datos_iniciales():
    ahora = datetime.now()
    base = [
        ("Revisar presupuesto regional", "Planilla compartida del área", 1, "ALTA", ahora + timedelta(days=2)),
        ("Vacuna antigripal", None, 2, "MEDIA", ahora + timedelta(days=6)),
        ("Cambiar filtro del agua", "Modelo FX-20", 3, "BAJA", None),
    ]
    resultado = {}
    for i, (titulo, desc, cat, prio, limite) in enumerate(base, 1):
        ident = str(uuid.UUID(int=i))
        resultado[ident] = {
            "id": ident, "titulo": titulo, "descripcion": desc, "categoriaId": cat,
            "prioridad": prio, "estado": "PENDIENTE", "estadoFecha": None,
            "fechaLimite": iso(limite) if limite else None, "avisoMinutos": None, "ubicacion": None,
            "creadoEn": iso(ahora - timedelta(days=1)), "actualizadoEn": iso(ahora - timedelta(days=1)),
        }
    return resultado


def cargar():
    if ARCHIVO.exists():
        return json.loads(ARCHIVO.read_text(encoding="utf-8"))
    return datos_iniciales()


def guardar_archivo():
    ARCHIVO.write_text(json.dumps(recordatorios, ensure_ascii=False, indent=2), encoding="utf-8")


recordatorios = cargar()


def validar(dto, ident):
    if dto.get("id") != ident:
        return "El id del cuerpo no coincide con el de la URL"
    titulo = (dto.get("titulo") or "").strip()
    if not 4 <= len(titulo) <= 60:
        return "El título debe tener entre 4 y 60 caracteres"
    if dto.get("descripcion") and len(dto["descripcion"]) > 200:
        return "La descripción supera los 200 caracteres"
    if dto.get("prioridad") not in PRIORIDADES:
        return "Prioridad desconocida"
    if dto.get("estado", "PENDIENTE") not in ESTADOS:
        return "Estado desconocido"
    if dto.get("categoriaId") not in CATEGORIAS:
        return "Categoría inexistente"
    return None


class Manejador(BaseHTTPRequestHandler):
    server_version = "RecordatoriosSimulado/1.0"

    def log_message(self, formato, *args):
        print(f"[{datetime.now():%H:%M:%S}] {self.address_string()} {formato % args}", flush=True)

    def responder(self, codigo, cuerpo=None):
        datos = b"" if cuerpo is None else json.dumps(cuerpo, ensure_ascii=False).encode("utf-8")
        self.send_response(codigo)
        if cuerpo is not None:
            self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(datos)))
        self.end_headers()
        if datos:
            self.wfile.write(datos)

    def leer_json(self):
        largo = int(self.headers.get("Content-Length") or 0)
        try:
            return json.loads(self.rfile.read(largo) or b"{}")
        except json.JSONDecodeError:
            return None

    def simular_fallas(self):
        """Devuelve True si ya respondió por el modo de falla activo."""
        if self.path.startswith("/admin") or self.path == "/salud":
            return False
        if modo == "error500":
            self.responder(500, {"error": "Error interno simulado"})
            return True
        if modo == "lento":
            time.sleep(20)
        return False

    # ---------- GET ----------
    def do_GET(self):
        if self.path == "/salud":
            return self.responder(200, {"ok": True})
        if self.path == "/admin/estado":
            with bloqueo:
                return self.responder(200, {"modo": modo, "total": len(recordatorios), "contadores": contadores})
        if self.simular_fallas():
            return
        if self.path == "/recordatorios":
            with bloqueo:
                contadores["GET"] += 1
                lista = sorted(recordatorios.values(), key=lambda r: r["creadoEn"] or "")
            return self.responder(200, lista)
        self.responder(404, {"error": "Ruta inexistente"})

    # ---------- PUT (crear o reemplazar: idempotente) ----------
    def do_PUT(self):
        if self.simular_fallas():
            return
        coincidencia = RUTA_ITEM.match(self.path)
        if not coincidencia:
            return self.responder(404, {"error": "Ruta inexistente"})
        ident = coincidencia.group(1)
        dto = self.leer_json()
        if dto is None:
            return self.responder(400, {"error": "JSON inválido"})
        with bloqueo:
            error = "Rechazo simulado" if modo == "rechazar" else validar(dto, ident)
            if error:
                contadores["PUT_rechazados"] += 1
                return self.responder(422, {"error": error})
            existia = ident in recordatorios
            dto.setdefault("actualizadoEn", iso(datetime.now()))
            recordatorios[ident] = dto
            contadores["PUT_reemplazados" if existia else "PUT_creados"] += 1
            guardar_archivo()
        self.responder(200 if existia else 201, dto)

    # ---------- DELETE ----------
    def do_DELETE(self):
        if self.simular_fallas():
            return
        coincidencia = RUTA_ITEM.match(self.path)
        if not coincidencia:
            return self.responder(404, {"error": "Ruta inexistente"})
        with bloqueo:
            if recordatorios.pop(coincidencia.group(1), None) is None:
                return self.responder(404, {"error": "No existe"})
            contadores["DELETE"] += 1
            guardar_archivo()
        self.responder(204)

    # ---------- Administración ----------
    def do_POST(self):
        global modo, recordatorios
        if self.path == "/admin/modo":
            nuevo = (self.leer_json() or {}).get("modo")
            if nuevo not in {"normal", "error500", "lento", "rechazar"}:
                return self.responder(400, {"error": "Modo desconocido"})
            modo = nuevo
            print(f"*** Modo cambiado a: {modo}", flush=True)
            return self.responder(200, {"modo": modo})
        if self.path == "/admin/reiniciar":
            with bloqueo:
                recordatorios = datos_iniciales()
                for clave in contadores:
                    contadores[clave] = 0
                guardar_archivo()
            return self.responder(200, {"total": len(recordatorios)})
        self.responder(404, {"error": "Ruta inexistente"})


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--puerto", type=int, default=8080)
    args = parser.parse_args()
    if not ARCHIVO.exists():
        guardar_archivo()
    servidor = ThreadingHTTPServer((args.host, args.puerto), Manejador)
    print(f"Servidor simulado en http://{args.host}:{args.puerto}/ ({len(recordatorios)} recordatorios)", flush=True)
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
