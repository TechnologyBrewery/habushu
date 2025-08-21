#HABUSHU_BUILDER_STAGE - HABUSHU GENERATED CODE (DO NOT MODIFY)
FROM docker.io/python:3.12 AS habushu_builder
USER root
RUN --mount=type=bind,source=target/containerize-support,target=/tmp/wheels \
    python3 -m venv /opt/venv && \
    /opt/venv/bin/python -m ensurepip --upgrade && \
    /opt/venv/bin/python -m pip install  \
    /tmp/wheels/habushu_poetry_package-3.3.0.dev0-py3-none-any.whl \
    /tmp/wheels/habushu_poetry_package_consumer-3.3.0.dev0-py3-none-any.whl 

#HABUSHU_BUILDER_STAGE - HABUSHU GENERATED CODE (END)

#HABUSHU_FINAL_STAGE - HABUSHU GENERATED CODE (DO NOT MODIFY)
FROM docker.io/python:3.12-slim AS final

# Copy venv from builder and activate by adding to the path
COPY --from=habushu_builder --chown=1001 --chmod=744 /opt/venv /opt/venv/
ENV PATH="/opt/venv/bin:$PATH"
#HABUSHU_FINAL_STAGE - HABUSHU GENERATED CODE (END)

CMD ["python", "-m", "habushu_poetry_package_consumer.verify_install"]