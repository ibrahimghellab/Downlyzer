# construction avec maven, puis une image légère avec juste le jre pour lancer le jar
FROM maven:3.9-eclipse-temurin-21 AS construction
WORKDIR /app
# les dépendances d'abord, pour que docker les garde en cache tant que le pom change pas
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# les tests tournent en local et pas ici : DownlyzerApplicationTests a besoin d'une base
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=construction /app/target/*.jar downlyzer.jar
# même fuseau que la base, sinon les seuils 48 h / 72 h sont décalés
ENV TZ=Europe/Paris
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "downlyzer.jar"]
