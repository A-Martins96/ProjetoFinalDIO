# Orçamento IA

## Sobre o projeto
Este projeto é uma API simples de orçamento financeiro que permite registrar transações e processar comandos por voz utilizando Spring AI e Tool Calling. O objetivo é demonstrar o fluxo principal: áudio → transcrição → IA → ferramenta → transação → resposta.

## Tecnologias
- Java
- Spring Boot
- Spring AI
- JPA
- Banco de dados H2
- Maven
- OpenAI / provider configurado via Spring AI

## Como executar
```bash
mvn clean install
mvn spring-boot:run
```

## Configuração
Defina a chave da API antes de iniciar a aplicação:

```bash
export OPENAI_API_KEY=sua_chave_aqui
```

No Windows PowerShell:

```powershell
$env:OPENAI_API_KEY="sua_chave_aqui"
```

## Endpoints
### Transações
- POST /transactions
- GET /transactions
- GET /transactions/{id}

### Áudio
- POST /audio

## Exemplo
Comando de voz:

"Registre uma despesa de 50 reais com alimentação."

Resultado esperado:

```json
{
  "message": "Despesa de R$ 50,00 registrada com sucesso."
}
```

## Fluxo
Áudio → Transcrição → IA → Tool → Transação → Resposta.

## Melhoria realizada
A aplicação foi evoluída para permitir o processamento de comandos financeiros por voz utilizando IA e Tool Calling, com integração simples e funcional para cadastro e consulta de transações.
