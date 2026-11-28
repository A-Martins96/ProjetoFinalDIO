# Orçamento IA

API REST em Java que registra receitas e despesas a partir de frases em português, faladas ou escritas, como *"gastei 35 reais no almoço"*. Usa Spring AI com Tool Calling e transcrição de áudio, e continua funcionando sem a IA.

**Stack:** Java 21 · Spring Boot 3.3 · Spring AI 1.0 · Spring Data JPA · H2 · Maven · JUnit 5

## Sobre

Anotar gastos é chato, e quem não anota perde o controle. Eu queria que registrar um gasto fosse tão simples quanto falar uma frase.

O projeto começou como trabalho final do bootcamp de Java da DIO e continuei evoluindo por conta própria.

## Como funciona

```
Áudio ──► Transcrição (Whisper) ──► Modelo de linguagem ──► ferramenta criarTransacao ──► TransactionService ──► banco
                                       │
                                       └── sem chave ou IA fora do ar: interpretador local (regex)
```

- **Tool Calling:** o modelo recebe a ferramenta `criarTransacao` (`ChatClient...tools(this)`) e decide chamá-la com descrição, valor, categoria e tipo.
- **A IA não grava nada sozinha:** quem converte o valor, valida e salva é o meu código (`ValorParser` e `TransactionService`).
- **Funciona offline:** sem `OPENAI_API_KEY`, um interpretador local entende os formatos mais comuns de frase.
- **Valores no formato brasileiro:** `50`, `35,90`, `1.500`, `1.500,75` e também `50.5`.

## Decisões

| Decisão | Por quê |
|---|---|
| Interpretador local como reserva | A API não pode parar porque um serviço externo caiu |
| Conversão de valor no meu código, não na IA | Dinheiro não pode depender de o modelo acertar o formato |
| Categorias e tipos como `enum` | O modelo só consegue gravar valores válidos |
| `.txt` aceito no `/audio` | Permite testar o fluxo inteiro sem chave e sem custo |
| H2 em memória | Rodar o projeto com um comando, sem instalar banco |

## Como executar

**Requisitos:** Java 21 ou mais novo e Maven.

```bash
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. Para usar a IA e a transcrição de voz, defina a chave antes:

```bash
export OPENAI_API_KEY=sua_chave          # PowerShell: $env:OPENAI_API_KEY="sua_chave"
```

### Exemplos

```bash
# Sem chave: frase em texto
curl -X POST localhost:8080/audio -F "file=@exemplos/teste-audio.txt"

# Com chave: áudio de verdade (mp3, m4a, wav, webm...)
curl -X POST localhost:8080/audio -F "file=@meu-comando.m4a"

curl localhost:8080/transactions
```

### Endpoints

| Método | Rota | Função |
|---|---|---|
| POST | `/transactions` | cria transação |
| GET | `/transactions` | lista |
| GET | `/transactions/{id}` | detalhe |
| POST | `/audio` | recebe áudio (ou .txt), transcreve e processa o comando |

O console do banco fica em `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:orcamento`, usuário `sa`, sem senha).

## Testes

```bash
mvn test
```

26 testes: conversão de valores (`ValorParserTest`), regras de transação (`TransactionServiceTest`), frases no interpretador local e o endpoint `/audio` (`AiFinanceIntegrationTest`). Os testes de integração são pulados quando há uma chave real no ambiente, para não gerar custo.

## Estrutura

```
src/main/java/com/dio/orcamentoai/
├── controller/   TransactionController, AudioController
├── service/      AiFinanceService, TransactionService, TranscriptionService, ValorParser
├── domain/       entidade Transaction, DTOs e enums de tipo e categoria
├── repository/   TransactionRepository (Spring Data JPA)
└── exception/    erros de negócio e handler global
exemplos/         frase de exemplo para testar o /audio sem chave
```

## O que eu corrigi revisando meu próprio código

Na primeira versão, o README prometia mais do que o código fazia:

1. A ferramenta `criarTransacao` existia, mas não era enviada ao modelo. Quem registrava era só o plano B.
2. O `/audio` lia o arquivo como texto. Não havia transcrição.
3. `"50.5"` virava `505`, porque o ponto era removido antes de tratar a vírgula.

Tudo funcionava na demonstração, e foi por isso que passou. Hoje, quando algo funciona de primeira, eu pergunto "funciona por quê?".

## Autor

**André Martins** · [Portfólio](https://andre-smartins.vercel.app) · [LinkedIn](https://www.linkedin.com/in/andre-smartins/) · [GitHub](https://github.com/A-Martins96)

## Licença

[MIT](LICENSE)
