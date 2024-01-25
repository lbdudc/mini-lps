#!/bin/bash

lpsFolder=$(pwd)
logFile=$lpsFolder/scripts/checkProductsLog.txt

client_=true
server_=true

case "$1" in
    "only-client") server_=false ;;
    "only-server") client_=false ;;
esac

echo "** Running script to check products **"$'\n'

# Cleaning log file
echo "" > $logFile

cd $lpsFolder

echoWithLog () {
  echo $'\n'"$@"$'\n' | tee -a $logFile
}

log () {
  echo $'\n'"$@"$'\n' &>> $logFile
}

# Installing root dependencies
npm install &>> $logFile \
  || ((echoWithLog "Error installing dependencies from root folder") && exit)

for f in $lpsFolder/products/*.json; do
	echo Checking product $(basename "$f");
	cd $lpsFolder

	# Generating project
	(npm run generate $f no-lint | tee -a $logFile) \
	  | grep -q 'Error: Command failed:' \
	  && (echoWithLog "Product generation" $(basename "$f") "failed") && continue

	if $server_
	then
		# Checking backend
		cd ./output/server
		chmod +x gradlew
		$(./gradlew build --exclude-task test &>> $logFile) && serverOK=true || serverOK=false
		if $serverOK
		then
			echo $'\t'Server built correctly \
			  && log Server built correctly for product $(basename "$f")
		else
			echo $'\t'Server build failed! \
			  && log Server build failed for product $(basename "$f")
		fi
		cd $lpsFolder
  fi

  if $client_
	then
		# Checking frontend
		cd ./output/client
		$(npm install &>> $logFile) && $(npm run build &>> $logFile) && clientOK=true || clientOK=false
		if $clientOK
		then
			echo $'\t'Client built correctly$'\n' \
			  && log Client built correctly for product $(basename "$f")
		else
			echo $'\t'Client build failed!$'\n' \
			  && log Client build failed for product $(basename "$f")
		fi
	fi
done

echo "** Script execution finished **"
