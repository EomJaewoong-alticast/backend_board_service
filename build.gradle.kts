import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
}

group = "com.msp"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

var kotlinxCoroutinesReactorVersion: String by extra

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")					// Kotlin 리플렉션 라이브러리(Spring Framework 5에서 필수)
	implementation("org.jetbrains.kotlin:kotlin-stdlib")					// Kotlin 표준 라이브러리의 Java8 변형
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${kotlinxCoroutinesReactorVersion}")		// Reactor utility
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")	// Kotlin Extension
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")	// Kotlin용 jackson

	implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	implementation("org.springframework.boot:spring-boot-starter-validation")

	// swagger
	implementation("io.github.cdimascio:openapi-spring-webflux-validator:3.3.0")

	// test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
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
