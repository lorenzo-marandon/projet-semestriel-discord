# Documentation guide

The canonical documentation source is [`index.adoc`](index.adoc). It describes the
currently implemented Discord foundation and clearly marks REST CRUD functionality as
planned for ticket #17.

## Source layout

- [`00-core/vision.adoc`](00-core/vision.adoc): current purpose and delivery boundary.
- [`00-core/glossary.adoc`](00-core/glossary.adoc): Discord and architecture vocabulary.
- [`00-core/NFR.adoc`](00-core/NFR.adoc): verified non-functional requirements.
- [`00-core/architecture.adoc`](00-core/architecture.adoc): implemented layers and design
  decisions.
- [`00-core/use-case-diagram.puml`](00-core/use-case-diagram.puml): implemented foundation
  versus planned CRUD scope.
- [`30-architecture/component-diagram.puml`](30-architecture/component-diagram.puml):
  implemented components.
- [`30-architecture/domain-class-diagram.puml`](30-architecture/domain-class-diagram.puml):
  domain and persistence model.
- [`30-architecture/technical-dependencies-diagram.puml`](30-architecture/technical-dependencies-diagram.puml):
  runtime and test dependencies.

The obsolete starter epic, sequences, and Cucumber traceability generator were removed.
No fictional Discord user stories or sequence diagrams replace them.

## Generate HTML and PDF

From the repository root:

```bash
./mvnw verify -Pdocs
```

The Maven profile processes `docs/index.adoc` and writes:

- `target/living-docs/html/index.html`
- `target/living-docs/pdf/index.pdf`

PlantUML diagrams are rendered by Asciidoctor Diagram. Generated files live under
`target/` and are intentionally ignored by Git.

## Maintenance rules

- Document only components and behavior present in the repository.
- Mark Discord REST resources and services as planned until ticket #17 implements them.
- Keep PlantUML references relative to `docs/index.adoc`.
- Run `mvn verify -Pdocs` after changing AsciiDoc or PlantUML sources.
- Check both generated outputs before committing documentation changes.
