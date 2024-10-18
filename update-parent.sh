liquibase-3.2.3/liquibase \
      --driver=org.postgresql.Driver \
      --classpath=/home/jbilling/postgresql-9.3-1102-jdbc41.jar \
      --url="jdbc:postgresql://localhost:5432/jbilling_test" \
      --changeLogFile=$1 \
      --username=jbilling \
      --password= \
update \
      -Dparent.entity.id=$2 
