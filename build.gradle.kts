plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.gukin.einvestlab"
version = "0.0.1-SNAPSHOT"
description = "e-invest-lab"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.ai:spring-ai-starter-model-openai:2.0.0-M2")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("com.mysql:mysql-connector-j")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

fun Test.configureExperiment(
    heap: String,
    label: String,
    extraEnv: Map<String, String> = emptyMap(),
) {
    useJUnitPlatform()
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter {
        includeTestsMatching("dev.gukin.einvestlab.company.CorpCodeFetchSmokeTestV3")
    }
    minHeapSize = heap
    maxHeapSize = heap
    environment("EXPERIMENT_HEAP_LABEL", label)
    environment("DART_API_KEY", System.getenv("DART_API_KEY") ?: "")
    environment("DART_BASE_URL", System.getenv("DART_BASE_URL") ?: "")
    extraEnv.forEach { (k, v) -> environment(k, v) }
    outputs.upToDateWhen { false }
    testLogging { showStandardStreams = true }
}

// A: heap sweep, batch=1000, iter=1
listOf("32m", "64m", "128m", "256m").forEach { heap ->
    tasks.register<Test>("memTest$heap") {
        configureExperiment(heap = heap, label = heap)
    }
}

// B: iteration sweep at multiple heap limits, batch=1000
listOf("32m", "64m", "128m").forEach { heap ->
    tasks.register<Test>("memTestB-$heap-iter10") {
        configureExperiment(
            heap = heap,
            label = "$heap-iter10",
            extraEnv = mapOf("EXPERIMENT_ITERATIONS" to "10"),
        )
    }
}

// C: batch-size sweep at fixed heap=64m, iter=1
listOf(100, 500, 1000, 5000, 10000).forEach { batchSize ->
    tasks.register<Test>("memTestC-batch$batchSize") {
        configureExperiment(
            heap = "64m",
            label = "64m-batch$batchSize",
            extraEnv = mapOf("EXPERIMENT_BATCH_SIZE" to batchSize.toString()),
        )
    }
}

// F: DB on vs off — does JDBC batchUpdate add floor memory?
(1..5).forEach { trial ->
    tasks.register<Test>("memTestF-dbon-t$trial") {
        configureExperiment(heap = "64m", label = "64m-dbon-t$trial")
    }
    tasks.register<Test>("memTestF-dboff-t$trial") {
        configureExperiment(
            heap = "64m",
            label = "64m-dboff-t$trial",
            extraEnv = mapOf("EXPERIMENT_SKIP_DB" to "true"),
        )
    }
}
tasks.register("memTestF-all") {
    dependsOn((1..5).flatMap { t -> listOf("memTestF-dbon-t$t", "memTestF-dboff-t$t") })
}

// E: repeated trials of C — variance check (5 runs per batch size)
listOf(100, 500, 1000, 5000, 10000).forEach { batchSize ->
    (1..5).forEach { trial ->
        tasks.register<Test>("memTestE-batch${batchSize}-t${trial}") {
            configureExperiment(
                heap = "64m",
                label = "64m-batch${batchSize}-t${trial}",
                extraEnv = mapOf("EXPERIMENT_BATCH_SIZE" to batchSize.toString()),
            )
        }
    }
}
tasks.register("memTestE-all") {
    dependsOn(listOf(100, 500, 1000, 5000, 10000).flatMap { b ->
        (1..5).map { t -> "memTestE-batch${b}-t${t}" }
    })
}

// D: input-size sweep. 1x = original zip, 10x/100x = synthesized first.
listOf(10, 100).forEach { mul ->
    tasks.register<JavaExec>("synthZip-${mul}x") {
        mainClass.set("dev.gukin.einvestlab.company.CorpCodeSyntheticZip")
        classpath = sourceSets["test"].runtimeClasspath
        args(mul.toString())
        maxHeapSize = "512m"
    }
}
listOf(1, 10, 100).forEach { mul ->
    tasks.register<Test>("memTestD-${mul}x") {
        if (mul > 1) dependsOn("synthZip-${mul}x")
        configureExperiment(
            heap = "64m",
            label = "64m-input${mul}x",
            extraEnv = mapOf("EXPERIMENT_INPUT_MULTIPLIER" to mul.toString()),
        )
    }
}
