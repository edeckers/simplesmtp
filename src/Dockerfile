FROM openjdk:17-alpine
 
COPY build/libs/KtSmtp-1.0-SNAPSHOT.jar /kt-smtp.jar

ENV KT_SMTP_PORT=9999

CMD ["sh", "-c", "java -jar /kt-smtp.jar -p ${KT_SMTP_PORT}"]