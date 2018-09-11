rm -f prime-prod-values.yaml temp.yml  
( echo "cat <<EOF >prime-prod-values.yaml";
  cat prime-prod-values-template.yaml;
  echo "EOF";
) >temp.yml
. temp.yml
