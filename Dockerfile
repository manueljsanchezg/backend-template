FROM ghcr.io/graalvm/native-image-community:25.0.2-ol8-20260120 AS builder

WORKDIR /app

COPY . .

RUN chmod +x mvnw

RUN ./mvnw package -Pnative -DskipTests

FROM debian:bookworm-slim

WORKDIR /app

COPY --from=builder /app/target/demo /app/demo

ENTRYPOINT ["/app/demo"]