# How 2 debug

./mvnw clean package -DskipTests

Afterwards:

cd spider

java -cp "target/classes:target/spider-1.0.0.jar:/home/codespace/.m2/repository/jdbm/jdbm/1.0/jdbm-1.0.jar"      com.comp4321.search.SearchTest