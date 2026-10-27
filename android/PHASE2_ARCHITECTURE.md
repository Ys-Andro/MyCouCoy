# Fase 2: Android autónomo y PC opcional

## Objetivo

**MyCouCoy debe funcionar sin una computadora.** Vincular una computadora
amplía las capacidades disponibles, pero nunca es un requisito para hablar
con los modelos configurados en el teléfono.

Esta fase reorganiza el código existente. No añade herramientas de agente,
permisos nuevos, sincronización de claves ni acceso adicional al dispositivo.

## Arquitectura y límites

```text
                    UI Android (Chat, Mochi, isla, Ajustes)
                                    |
                                AppModel
                         (orquestación de UI)
                           /                \
               ChatRouting                   DesktopLink opcional
               (regla pura)                   (emparejamiento, sesiones,
                   |                           aprobaciones, modelos PC)
          PhoneAssistantSession                     |
          (contexto por proveedor)                PC Coucou
                   |
          AssistantTransport
                   |
           CloudAssistant (HTTPS)
                /       \
            Gemini      NVIDIA
```

### Responsabilidades

- **ChatRouting.kt**: selecciona el destino del mensaje a partir de
  configuración local, preferencia guardada y disponibilidad de la PC.
  No conoce claves, Android, sockets ni tipos de protocolo.
- **PhoneAssistantSession.kt**: construye las solicitudes del teléfono,
  aísla el contexto de cada proveedor y gestiona su transporte cancelable.
  No importa `DesktopLink` ni las clases de la UI.
- **AssistantTransport / CloudAssistant.kt**: separa el contrato del
  envío de su implementación por HTTPS. Esto permite usar un transporte
  falso en pruebas sin acceso a Internet.
- **AppModel.kt**: conserva las señales observables de la UI y
  coordina el chat visible, persistencia y Mochi/isla; delega el contexto
  y las solicitudes móviles a `PhoneAssistantSession`.
- **DesktopLink**: sin modificaciones de protocolo, seguridad de
  emparejamiento ni autorizaciones. No recibe claves API del teléfono.

## Decisiones de enrutamiento

| Situación | Destino |
| --- | --- |
| Solo teléfono configurado, sin PC | IA del teléfono |
| PC configurada y disponible, elegida explícitamente | PC |
| PC elegida, se desconecta, teléfono configurado | IA del teléfono |
| La PC vuelve a estar disponible y conserva el modelo | Modelo de PC previamente elegido |
| IA del teléfono elegida, PC se conecta | IA del teléfono |
| Sin IA local y PC desconectada | No se puede enviar |
| Modelo remoto no anunciado o no autorizado | No se puede enviar a ese modelo |

La preferencia guardada (misma clave `chat_model`) no se sobrescribe durante
el fallback. Por eso la ruta vuelve a la elección previa al recuperarse
la conexión. En ningún caso se envía una clave API del teléfono a la PC.

## Compatibilidad

- Se conservan el archivo de chat y su formato, las preferencias existentes,
  las claves cifradas en Android Keystore y el selector de proveedor.
- Se mantiene la conversación visible, la cancelación, los mensajes de
  error, el aviso de pérdida de Internet, voz y animación de Mochi.
- Se conservan QR, emparejamiento, aprobación biométrica, sesiones, relay,
  capacidades y comandos del escritorio.
- No se migra ni combina el contexto privado de Gemini o NVIDIA con una
  conversación remota; `ProviderHistory` sigue separado por proveedor.
- Las pruebas de `ChatRoutingTest` cubren fallback y restauración;
  `PhoneAssistantSessionTest` comprueba autonomía, aislamiento,
  validación de credenciales, cancelación y borrado.

## Validación

Ejecutar en la carpeta `android`:

```powershell
.\gradlew.bat test assembleDebug lint --console=plain
```

Probar en un teléfono:
1. Sin PC vinculada: enviar texto, usar voz, comprobar isla y cancelar.
2. Vincular PC y elegir un modelo de PC; verificar que responde desde PC.
3. Desconectar PC, sin cerrar MyCouCoy: el modelo del teléfono debe quedar
   disponible para el próximo mensaje.
4. Reconectar PC: debe reaparecer la elección guardada si se anuncia el
   mismo modelo. Cambiar manualmente a IA del teléfono y verificar que
   no vuelve a cambiar por sí solo.
5. Verificar que las aprobaciones del PC continúan teniendo el
   mismo paso de confirmación, y que una falla del PC no afecta
   la conversación del teléfono.

**Fuera del alcance:** agentes con ejecución de acciones,
automatizaciones programadas, memoria persistente ampliada, análisis
de pantalla y coordinación autónoma entre agentes de PC y Android.
Corresponden a fases posteriores.
