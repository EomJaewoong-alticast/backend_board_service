import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar
import com.bmuschko.gradle.docker.tasks.image.*

/*
 * 미리 구성해 둔 task 그룹(...?)
 * 특정 빌드 과정에서 필요한 기본정보
 * kotlin: kotlin 전용 id -> id('org.jetbrains.kotlin.<...>')
 */
plugins {
	id("org.springframework.boot") version "2.3.12.RELEASE"			// SpringBoot 사용
	id("io.spring.dependency-management") version "1.0.9.RELEASE"	// 스프링 관련 의존성의 버전관리를 일괄적으로 하기 위한 플러그인
	kotlin("jvm") version "1.4.32"								// jvm 플랫폼 타겟 명시
	kotlin("plugin.spring") version "1.4.32"					// kotlin으로 스프링 개발시 필수 플러그인

	id("com.bmuschko.docker-remote-api") version "7.1.0"
}

apply(plugin = "com.bmuschko.docker-remote-api")

var kotlinxCoroutinesReactorVersion: String by extra

java {
	java.sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

group = "com.msp"
version = "0.0.1-SNAPSHOT"
var registry = System.getenv("APP_REGISTRY") ?: ""
if (registry.isNotEmpty()) registry += "/"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")													// Kotlin 리플렉션 라이브러리(Spring Framework 5에서 필수)
	implementation("org.jetbrains.kotlin:kotlin-stdlib")													// Kotlin 표준 라이브러리의 Java8 변형
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${kotlinxCoroutinesReactorVersion}")	// Reactor utility
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")									// Kotlin Extension
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")									// Kotlin용 jackson

	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")					// mongodb
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")						// redis
  
	implementation("org.springframework.boot:spring-boot-starter-webflux")									// webflux
	implementation("org.springframework.boot:spring-boot-gradle-plugin:2.5.0")

	// validation
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// spring cloud
	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client:2.2.7.RELEASE")

	// swagger
	implementation("io.github.cdimascio:openapi-spring-webflux-validator:3.3.0")

	// test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
}

// 실행 가능한 jar 파일 생성
val jar: Jar by tasks
val bootJar: BootJar by tasks

bootJar.enabled = true
jar.enabled = false

tasks.bootJar {
	println("Start bootJar...")
	// MANIFEST.MF 생성 - 애플리케이션에 대한 구성, 확장, 클래스 로더 및 서비스 등을 등록
	// META_INF 디렉터리에 생성됨
	manifest {
		attributes["Title"] = "Mobility Service Platform"
		attributes["Module"] = project.name
		attributes["Built-By"] = System.getProperty("user.name")
		attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
		attributes["Build-JDK"] = "${System.getProperty("java.version")} ${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")}"
		attributes["Build-OS"] = "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}"
	}
}

// docker file 생성
val springProfile: String by project
val createDockerfile by tasks.creating(Dockerfile::class) {
	destFile.set(project.file("./build/docker/Dockerfile"))
	val javaOpts = ""
	val execJar = "${project.name}-${version}.jar"
	val profile = if(springProfile.isNullOrEmpty()) "default" else springProfile // gradle.properties에 정의됨
	val RAIDEA_PROFILE = profile

	from("openjdk:11-slim")
	exposePort(39001)
	workingDir("/opt/${project.name}/libs/")
	runCommand("pwd")
	copyFile ("./build/libs/${execJar}", "/opt/${project.name}/libs/${execJar}")
	runCommand("touch /opt/${project.name}/libs/${execJar}")

	environmentVariable("RAIDEA_PROFILE", "default")
	entryPoint ("sh", "-c", "java ${javaOpts} -Dspring.profiles.active=${RAIDEA_PROFILE} -Djava.security.egd=file:/dev/./urandom -jar ${execJar}")
}

// docker image 빌드
tasks.create("buildDockerImage", DockerBuildImage::class) {
	println("Start buildDockerImage...")
	dependsOn(createDockerfile)
	dockerFile.set(createDockerfile.destFile)
	println("Set created Dockerfile...")
	inputDir.set(project.projectDir)
	println("Set Input ProjectDir...${project.projectDir}")
	println("image name : ${registry}${project.name}:${version}")
	println("image name : ${registry}${project.name}:latest")
	images.set(setOf("${registry}${project.name}:${version}", "${registry}${project.name}:latest"))
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
