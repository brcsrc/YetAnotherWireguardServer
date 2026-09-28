# YAWS Typescript API Client

This package turns the OpenAPI spec into a node module the frontend consumes via a local package
reference. The frontend imports it as `@yaws/yaws-ts-api-client`.

## Normal usage

You do not need to run anything here directly. From the repository root:

```shell
make build-api-spec    # generates build/openapi.json from the Spring app
make build-api-client  # regenerates this client from that spec, then compiles it
```

`make build` runs both in order, along with the backend and frontend builds. Any change to a
controller, request model or response model needs both steps, since the frontend consumes the
compiled output rather than the Java types.

## What the steps do

**`make build-api-spec`** runs `OpenApiSpecGeneratorTest`, which loads the Spring MVC context
without binding to a port and writes `build/openapi.json`.

**`make build-api-client`** runs two npm scripts here:

- `npm run generate` copies `build/openapi.json` to `./openapi.json` and runs
  `openapi-generator-cli` over it, emitting typescript into `./src`. The spec is copied in rather
  than read from `build/` so the generator input is versioned next to its output.
- `npm run build` runs `tsc`, compiling `./src` into `build/client` with declarations in
  `build/client/types`.

Both the generated typescript in `./src` and the copied `./openapi.json` are committed, so a diff
shows exactly how an API change altered the client.

## Notes

**tsc is incremental, and its state lives outside this directory.** The build info file is
`build/tsconfig.tsbuildinfo`, not `src/client/`. If `build/client` is deleted without also removing
that file, `tsc` considers everything current, reports success and emits nothing — producing a
frontend build that fails on unresolved imports. The `build-api-client` make target removes it
before compiling, so use the make target rather than calling `npm run build` directly.

**The `servers` block in the spec is not meaningful.** Because the spec is generated from a test
context with no bound port, it declares `http://localhost` rather than `http://localhost:8080`, and
the generator writes that into `runtime.ts` as `BASE_PATH`. Nothing reads it: the API and the SPA
are colocated, so `src/frontend/src/api/HTTPClients.ts` sets `basePath` to `window.location.origin`
and requests go to whatever host and port served the page. A change to that line in a regenerated
diff is expected and harmless.
