# JReview

JReview is a tiny Java console application that classifies e-commerce reviews with a **local Ollama model**. It returns one of three labels: `VERIFIED`, `SPAM`, or `TOXIC`.

It runs in the terminal. There is no browser page, no web server, no API key, and no database. After the model is downloaded once, classification works fully offline. The goal is that you can read the whole project in a few minutes and explain it out loud.

---

## 1. What the project does

Type a review, or paste several reviews as JSON. JReview asks a local Ollama model to pick one label per review.

| Label | Meaning | Example |
|---|---|---|
| `VERIFIED` | A genuine first-hand opinion about the product. Positive **or negative**. | “Broke after two days. Very disappointed.” |
| `SPAM` | Promotion, affiliate links, giveaways, phishing, or fake content. | “Click my link to win a free iPhone!!” |
| `TOXIC` | Harassment, hate, threats, or personal abuse. | “Anyone who bought this is an idiot.” |

Two important clarifications:

1. **Negative reviews are not toxic or spam.** A real complaint is `VERIFIED`.
2. **`VERIFIED` does not mean verified purchase.** It only means the comment reads as a genuine first-hand opinion.

Out of scope for this first version: databases, authentication, queues, purchase verification, confidence scores, and model-written explanations.

---

## 2. Why a local model

JReview used to call a hosted decision API. It now runs a small instruction-tuned model through Ollama on your own machine.

| Hosted API | Local Ollama |
|---|---|
| Needs an API key and account | No key, no account |
| Sends review text off-machine | Text never leaves the machine |
| Per-request cost and rate limits | Free after the one-time model download |
| Requires internet at runtime | Works offline |

The tradeoff is that your hardware sets the speed. A 3B–4B model is plenty for a three-way label and runs comfortably on a normal laptop.

---

## 3. Architecture

```text
          Terminal menu
                │
                ▼
             ReviewCli
     (1) Custom input / (2) JSON multiple inputs
                │
                ▼
         ReviewInputParser
     (turns input into plain strings)
                │
                ▼
          ModerationService
        (owns the moderation rules)
                │
                ▼
           OllamaClient
   (one /api/chat call per review)
                │
                ▼
   Ollama daemon — http://localhost:11434
   POST /api/chat  →  {"verdict":"TOXIC|SPAM|VERIFIED"}
```

The important idea: **each class has one job**.

- The CLI does no moderation; it only reads input and prints output.
- The parser knows about the two input modes, but nothing about Ollama.
- The service owns the rules, but does no terminal I/O.
- The client knows HTTP and JSON, but not what the labels mean.

---

## 4. Project structure

### Main code

| File | Job |
|---|---|
| `JReviewApplication.java` | Starts the app without a web server, then launches the CLI. |
| `ReviewCli.java` | Shows the menu, reads both input modes, prints verdicts. |
| `ReviewInputParser.java` | Validates input and turns it into one or more plain review strings. |
| `ModerationService.java` | Holds the moderation rules and converts the model’s label to `Verdict`. |
| `OllamaClient.java` | Sends one constrained chat request to Ollama and returns the chosen label. |
| `Verdict.java` | Defines the three allowed labels. |

### Tests

| File | What it proves |
|---|---|
| `OllamaClientTest` | Sends the expected `/api/chat` request and parses the JSON answer. |
| `ModerationServiceTest` | Maps all three labels correctly and rejects unknown labels. |
| `ReviewInputParserTest` | Parses typed text and JSON arrays, and rejects invalid input. |
| `ReviewCliTest` | Both menu modes print verdicts; errors stay friendly. |
| `JReviewApplicationTests` | The Spring context starts without a web server. |

### Config

| File | Job |
|---|---|
| `application.properties` | Non-secret config: Ollama URL, model name, temperature, context size. |

---

## 5. Request flow, step by step

1. The app starts and shows a menu with options `1`, `2`, and `0`.
2. In **Custom input** mode, you type one review. In **JSON multiple inputs** mode, you paste an array and finish with an empty line.
3. `ReviewInputParser` validates the input and returns plain review string(s).
4. `ModerationService` writes the moderation rules into a system prompt with three choices.
5. `OllamaClient` posts one chat request to Ollama per review, with JSON output forced to a single `verdict` field.
6. Ollama returns `{"verdict":"TOXIC"}` in `message.content`.
7. `ModerationService` converts that string to `Verdict.TOXIC`.
8. `ReviewCli` prints the verdict and a short fixed explanation. In JSON mode it also prints a results table and a summary count.

---

## 6. Moderation rules

The rule order is written directly into the system prompt sent to Ollama:

1. **TOXIC first** — harassment, hate, threats, or personal abuse.
2. **SPAM second** — promotions, affiliate links, giveaways, phishing, or fake content.
3. **VERIFIED otherwise** — a genuine first-hand opinion.

The prompt explicitly says:

> A negative review is still VERIFIED if it is a real experience.

Why this matters: “This product is terrible and broke immediately” is `VERIFIED`, not `TOXIC`. Deciding otherwise would punish honest criticism.

Unknown labels do **not** become `VERIFIED`. The CLI reports that moderation is unavailable, so a bad model answer cannot silently approve content.

---

## 7. The Ollama request

`OllamaClient` builds this shape:

```json
{
  "model": "qwen3.5:4b",
  "messages": [
    {
      "role": "system",
      "content": "Classify this e-commerce review. ...\n\nRules:\n- TOXIC: ...\n- SPAM: ...\n- VERIFIED: ..."
    },
    { "role": "user", "content": "This product broke after two days." }
  ],
  "stream": false,
  "think": false,
  "format": {
    "type": "object",
    "properties": {
      "verdict": { "type": "string", "enum": ["VERIFIED", "SPAM", "TOXIC"] }
    },
    "required": ["verdict"]
  },
  "options": { "temperature": 0, "num_ctx": 1024 }
}
```

It sends:

```text
POST http://localhost:11434/api/chat
Content-Type: application/json
```

It reads only `message.content`, parses it as JSON, and takes `verdict`. The `format` schema and `temperature: 0` keep the answer short and repeatable; `think: false` avoids a long reasoning trace.

---

## 8. Tech stack and why

| Choice | Why |
|---|---|
| Java 25 | Installed locally and current. |
| Spring Boot 4.2.0 (non-web) | Gives us dependency injection and configuration with almost no boilerplate. |
| Spring `RestClient` | One readable HTTP call to Ollama, no SDK dependency. |
| Jackson `ObjectMapper` | Parses the JSON array typed into the console and the model’s answer. |
| Ollama | Runs small open models locally with a simple HTTP API. |
| JUnit + Mockito + MockRestServiceServer | Tests run locally without a model or network. |
| Maven wrapper | Works even when Maven is not installed system-wide. |

No Thymeleaf, no REST controllers, no JavaScript, and no cloud API key: the application is Java only.

---

## 9. Recommended model

**Download this one:**

```bash
ollama pull qwen3.5:4b
```

- About 3.3–4.0 GB, fits a 4 GB GPU with RAM overflow
- Strong instruction-following, which matters for the “negative review is still VERIFIED” rule
- Apache 2.0 license

**If it feels slow:**

```bash
ollama pull qwen3.5:2b
```

About 2.7–3.1 GB, faster and fully in VRAM, slightly less reliable on edge cases. The first download needs internet; after that, JReview is offline. You can switch models at any time with `OLLAMA_MODEL=...` (see Configuration).

---

## 10. Run it locally

```bash
ollama pull qwen3.5:4b
./mvnw spring-boot:run
```

Ollama normally starts its daemon automatically. If it is not running:

```bash
ollama serve
```

You can also build a jar and run it directly:

```bash
./mvnw clean package
java -jar target/JReview-0.0.1-SNAPSHOT.jar
```

The app prints its menu in the terminal. **It does not open an HTTP port.**

---

## 11. Console reference

### Menu

```text
1) Custom input
2) JSON multiple inputs
0) Exit
Choose an option:
```

### 1) Custom input

Type one review at the prompt:

```text
Choose an option: 1
Review text: Broke after two days. Very disappointed.

Review:  Broke after two days. Very disappointed.
Verdict: VERIFIED — genuine first-hand opinion (positive or negative)
```

### 2) JSON multiple inputs

Paste a JSON array of objects, each with a non-blank `text` field. Finish with an empty line:

```text
Choose an option: 2
Paste a JSON array of objects with a text field, for example:
[{"text":"Great product"},{"text":"Click my link!"}]
Finish with an empty line.
[
  {"text": "Great product"},
  {"text": "Click my link!"},
  {"text": "Anyone who bought this is an idiot."}
]

#    VERDICT    REVIEW
1    VERIFIED   Great product
2    SPAM       Click my link!
3    TOXIC      Anyone who bought this is an idiot.

Summary: 3 review(s) — VERIFIED 1, SPAM 1, TOXIC 1
```

Rules for this mode:

- The input must be a JSON **array**, not a single object.
- Every item must be an object with a non-blank string `text`.
- At most 100 reviews per batch.
- Each review is one Ollama call, so large batches take longer.

### Errors

```text
Error: Review text must not be blank.
Error: That is not valid JSON.
Error: JSON must be a non-empty array of objects with a "text" field.
Ollama is unavailable. Is 'ollama serve' running with the model pulled?
```

Bad input returns you to the menu; it never ends the app.

---

## 12. Configuration

Defined in `src/main/resources/application.properties`:

| Property | Default | Purpose |
|---|---|---|
| `ollama.base-url` | `http://localhost:11434` | Local Ollama daemon URL. |
| `ollama.model` | `qwen3.5:4b` | Model tag used for classification. |
| `ollama.temperature` | `0` | Keeps answers deterministic. |
| `ollama.num-ctx` | `1024` | Small context window for short reviews. |

Override without editing files using environment variables:

```bash
OLLAMA_MODEL=qwen3.5:2b ./mvnw spring-boot:run
OLLAMA_BASE_URL=http://localhost:11434 ./mvnw spring-boot:run
```

There is no secret to configure.

---

## 13. Testing

Run:

```bash
./mvnw clean verify
```

What is tested:

- The Ollama client sends the expected path, model, JSON schema, and non-streaming flags.
- A canned Ollama answer is parsed correctly; malformed answers are rejected.
- `TOXIC`, `SPAM`, and `VERIFIED` map to the enum.
- An unknown label throws an exception.
- Typed text and JSON arrays are parsed; blank, malformed, oversized, and missing-field input is rejected.
- Both CLI modes print verdicts and show friendly errors.
- The Spring context starts without a web server.

No real model call happens in tests, so no Ollama daemon is required.

---

## 14. Offline and privacy

- Once the model is pulled, JReview makes **no internet calls**.
- Review text is sent only to `localhost:11434`; it never leaves the machine.
- There is no API key, account, bill, or rate limit.
- Generation is local, so speed depends on your CPU/GPU; the first call also loads the model into memory.

---

## 15. Security notes

- No credentials exist in code, config, or the environment.
- Ollama listens on localhost by default; do not expose it to an untrusted network.
- Review text stays on the machine, which makes local moderation suitable for sensitive content.
- For production, add authentication and abuse protection before exposing this to untrusted users.

---

## 16. Limitations

This first version does not:

- verify purchases or orders;
- store reviews;
- provide model-written explanations;
- handle multiple languages explicitly;
- provide a human review queue;
- add confidence thresholds;
- call Ollama concurrently for a batch (reviews are classified one after another).

Possible next steps:

1. Add a confidence signal by asking the model for a score alongside the label.
2. Send low-confidence reviews to human review.
3. Parallelize batch calls if throughput matters.
4. Add structured logging without storing review text.
5. Add a GitHub Actions workflow running `./mvnw -B verify`.

---

## 17. Explain this project in 5 minutes

> “JReview is a Java console app with one terminal menu and two input modes. Custom input takes one review typed at the prompt; JSON multiple inputs takes an array of objects with a `text` field, finished with an empty line. `ReviewInputParser` turns both into plain strings, so the rest of the flow is shared. The service owns the moderation rules: toxic first, spam second, verified otherwise. A negative but genuine review is still verified. The client makes one local call to Ollama per review, with a JSON schema that forces the answer to one of three labels, and we print that label with a short fixed explanation. There is no API key and no data leaves the machine. Tests stub Ollama, so CI needs no model or network.”

---

## 18. Troubleshooting

| Problem | Likely cause | Fix |
|---|---|---|
| “Ollama is unavailable” | Daemon is not running | Run `ollama serve` in another terminal. |
| Model-not-found error | Model not pulled | Run `ollama pull qwen3.5:4b` (or your configured `OLLAMA_MODEL`). |
| Classification is slow | Model too large for the GPU, or first load | Try `OLLAMA_MODEL=qwen3.5:2b`; the first call loads the model. |
| Bad input shows an error | Validation worked as intended | Enter non-empty text or use `[{"text":"Your review"}]`. |
| Unknown label shows an error | Model returned an unexpected value | Check the prompt/model; JReview refuses to guess. |
| Tests fail before reaching Ollama | Java or Maven wrapper issue | Use Java 25 and run `./mvnw clean verify`. |
