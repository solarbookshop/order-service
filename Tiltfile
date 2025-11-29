# Build
custom_build(
    # Name of the container image
    ref = 'order-service',
    # Command to build the container image
    # On Windows, replace $EXPECTED_REF with %EXPECTED_REF%
    command = './gradlew bootBuildImage --imageName $EXPECTED_REF',
    # Files to watch that trigger a new build (full image rebuild)
    deps = [
        'build.gradle.kts',
        'src',
        # Also watch the local compiled outputs that we sync into the container
        'build/classes/java/main',
        'build/resources/main',
    ],
    # Live Update: sync compiled outputs directly into the running container
    live_update = [
        sync('build/classes/java/main', '/workspace/BOOT-INF/classes'),
        sync('build/resources/main', '/workspace/BOOT-INF/classes'),
        # If you don't use DevTools or Paketo live reload, uncomment a restart action:
        # run('sh -lc "pkill -f java || true"'),
    ]
)

# Deploy
k8s_yaml(['k8s/deployment.yml', 'k8s/service.yml'])

# Manage
k8s_resource('order-service', port_forwards=['9002'])