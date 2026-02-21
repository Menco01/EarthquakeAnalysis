# EarthquakeAnalysis
L’obiettivo del progetto è quello di realizzare una implementazione in Scala + Spark di un’analisi di cooccorrenza di terremoti in località diverse su un dataset specifico. 

## Prerequisiti
### Creazione VM
Creare ed avviare un'Istanza VM con Google Cloud Console e autenticarsi con il proprio account Google (con gcloud
auth login), seguendo le indicazioni sulla guida ufficiale: <br><br>
https://docs.cloud.google.com/compute/docs/instances/instance-creation-overview <br>
Per la fase di sviluppo e testing del progetto ho utilizzato una macchina di tipo _n2-standard-4_ con immagine _debian-12-bookworm-v20260210_ <br>
### Creazione Bucket
Creare un bucket servirà ad importare le risorse del progetto all'interno del proprio ambiente in locale. Per la creazione vedere la guida: <br><br>
https://docs.cloud.google.com/storage/docs/creating-buckets
## Ottenimento Risorse
Una volta entrati con SSH nella macchina virtuale, eseguire i seguenti comandi per inserire i file del progetto all'interno del proprio Bucket: <br><br>
`curl https://storage.googleapis.com/scalable-project-earthquake/EarthquakeAnalysis-assembly-1.0.jar | gcloud storage cp - gs://[NOME BUCKET]/[NOME FILE DESTINAZIONE].jar` 
##### FULL DATASET
`curl https://storage.googleapis.com/scalable-project-earthquake/dataset-earthquakes-full.csv | gcloud storage cp - gs://[NOME BUCKET]/dataset-earthquakes-full.csv` 
##### TRIMMED DATASET 
`curl https://storage.googleapis.com/scalable-project-earthquake/dataset-earthquakes-trimmed.csv | gcloud storage cp - gs://[NOME BUCKET]/dataset-earthquakes-trimmed.csv`
#### In caso di errore:
Nel caso in cui non dovesse funzionare il trasferimento da CLI, scaricare i file da questo link Drive per poi inserirli manualmente all'interno del proprio Bucket: <br><br>
https://drive.google.com/drive/folders/1_pXDhv92IwRL48yGY1OdceNBS3dLgunZ?usp=drive_link
## Creazione Cluster
Prima di eseguire il .jar bisogna configurare il cluster tramite questo comando: <br><br>
`gcloud dataproc clusters create [NOME CLUSTER]  --region=[REGIONE]  --num-workers [NUMERO WORKER NODE]  --master-boot-disk-size 240  --worker-boot-disk-size 240  --master-machine-type=n2-standard-4  --worker-machine-type=n2-standard-4` <br><br>
I test svolti sono stati fatti impostando un valore di [NUMERO WORKER NODE] di 2, 3 e 4 
#### Eliminazione Cluster
Se si vuole provare l'analisi con altre configurazioni di worker nodes, si consiglia di eliminare il cluster precedente utilizzando il comando: <br><br>
`gcloud dataproc clusters delete [NOME CLUSTER] --region=[REGIONE]`
## Esecuzione Analisi
Una volta aver caricato tutte le risorse nel bucket ed aver creato il cluster è possibile eseguire il .jar con il comando: <br><br>
`gcloud dataproc jobs submit spark --cluster=[NOME CLUSTER]   --region=[REGIONE] --jar=gs://[NOME BUCKET]/[NOME FILE JAR].jar -- gs://[NOME BUCKET]/[NOME FILE DATASET].csv [NUMERO PARTIZIONI]`
