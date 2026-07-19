# 💰 Kori

Asistente de finanzas personales que funciona por **WhatsApp**, con un dashboard web para visualizar tus datos.

Registras gastos escribiendo como hablas ("almuerzo 25") y Kori los clasifica, controla tus presupuestos y te avisa cuando te acercas a un límite.

🔗 **Dashboard:** https://kori-dashboard.onrender.com

> El backend usa el plan gratuito de Render y se suspende tras 15 minutos de inactividad.
> La primera petición puede tardar ~1 minuto en despertar el servicio.

## Qué hace

- **Registro por lenguaje natural** — "taxi 15.50", "sueldo 2000"
- **Clasificación híbrida** — reglas primero (rápido y gratis), IA solo para los casos ambiguos
- **Categorías dinámicas** — si la IA detecta una categoría nueva, la propone y espera tu confirmación
- **Presupuestos con semáforo** — 🟢 🟡 🔴 según el porcentaje usado
- **Onboarding con IA** — le dices tu sueldo y meta de ahorro, y propone el reparto por categoría
- **Alertas proactivas** — te avisa al registrar un gasto que cruza el 80% o el 100%
- **Insights** — la IA analiza tus patrones del mes y da observaciones (no asesoría financiera)
- **Dashboard** — balance, gráfico por categoría, presupuestos y movimientos, con modo día/noche

## Stack

**Backend:** Java 21 · Spring Boot · PostgreSQL · Docker
**Frontend:** Angular 22 (standalone, zoneless) · Chart.js
**Integraciones:** Twilio (WhatsApp) · DeepSeek (IA)
**Infraestructura:** Render · Neon · GitHub Actions

## Decisiones de diseño

**Reglas antes que IA.** La mayoría de mensajes se resuelven con regex y palabras clave. La IA solo entra cuando las reglas fallan: más rápido, más barato y más predecible.

**La IA nunca decide sola.** Toda respuesta del modelo se valida contra datos reales (las categorías existen, los montos cuadran) y hay un fallback sin IA por si el modelo falla.

**Observaciones, no asesoría.** Los insights describen patrones en tus datos; no recomiendan decisiones financieras.

**Estado conversacional.** Las sugerencias y planes pendientes se persisten, así el bot mantiene contexto entre mensajes.

## Limitaciones conocidas

- **API sin autenticación.** Kori está diseñado como app de un solo usuario. Los endpoints son públicos; el siguiente paso sería agregar autenticación.
- **Cold start.** El plan gratuito suspende el servicio; el primer mensaje tras inactividad puede perderse por timeout de Twilio.
- **Sandbox de Twilio.** Requiere unirse al sandbox para probar el bot.

## Correr localmente

```bash
cp .env.example .env   # completa tus credenciales
docker compose up --build
```

```bash
cd dashboard
npm install
npx ng serve
```

## Tests

```bash
./mvnw clean verify
```