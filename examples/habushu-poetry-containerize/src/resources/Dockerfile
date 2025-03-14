#HABUSHU_BUILDER_STAGE - HABUSHU GENERATED CODE (DO NOT MODIFY)
FROM docker.io/python:3.12 AS habushu_builder
# Poetry and supporting plugin installations
RUN python -m ensurepip --upgrade && \
    pip install poetry==2.0.1 && \
    poetry self add poetry-monorepo-dependency-plugin@1.2.0 && \
    poetry self add poetry-plugin-bundle@1.5.0

WORKDIR /work-dir
COPY --chown=1001 target/containerize-support ./containerize-support/
RUN find . -type f -name pyproject.toml -exec sed -i 's|develop[[:blank:]]*=[[:blank:]]*true|develop = false|g' {} \;

USER root
WORKDIR /work-dir/containerize-support/examples/add-habushu-to-new-or-existing-poetry-project
ENV POETRY_CACHE_DIR="/.cache/pypoetry"

# export target project's virtual environment to /opt/venv
RUN --mount=type=cache,target=/.cache/pypoetry/ \
    poetry lock && \
    poetry bundle venv /opt/venv
#HABUSHU_BUILDER_STAGE - HABUSHU GENERATED CODE (END)


#HABUSHU_FINAL_STAGE - HABUSHU GENERATED CODE (DO NOT MODIFY)
FROM docker.io/python:3.12-slim AS final
# Copy venv from builder and activate by adding to the path
COPY --from=habushu_builder --chown=1001 /opt/venv /opt/venv/
ENV PATH="/opt/venv/bin:$PATH"
#HABUSHU_FINAL_STAGE - HABUSHU GENERATED CODE (END)
