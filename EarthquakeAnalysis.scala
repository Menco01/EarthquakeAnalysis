import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd.RDD
import scala.math.BigDecimal.RoundingMode

object EarthquakeAnalysis {

  // Definizione di un tipo per le coordinate (Latitudine, Longitudine)
  type Location = (Double, Double)

  def main(args: Array[String]): Unit = {

    // Configurazione Spark Session
    val spark = SparkSession.builder
      .appName("Earthquake Co-occurrence Analysis")
      .getOrCreate()

    // Import impliciti per conversioni RDD/Dataset
    import spark.implicits._

    // Controllo Argomenti (Input Path e Numero Partizioni per Scalabilità)
    if (args.length < 1) {
      System.err.println("Missing structure: EarthquakeAnalysis <input-path> [num-partitions]")
      System.exit(1)
    }

    val inputPath = args(0)
    // Default a 4 partizioni
    val numPartitions = if (args.length > 1) args(1).toInt else 4

    println(s"Processing file: $inputPath with $numPartitions partitions")

    // Lettura del CSV
    // Utilizziamo il lettore CSV di Spark per gestire l'header
    val rawData = spark.read
      .option("header", "true")
      .csv(inputPath)
      .rdd

    // Pre-processing e Map
    // Estrarre Key=((Lat, Lon), Date) per rimuovere i duplicati
    val formattedData: RDD[(Location, String)] = rawData.flatMap(row => {
      try {
        // Parsing delle stringhe
        val latStr = row.getAs[String]("latitude")
        val lonStr = row.getAs[String]("longitude")
        val dateFull = row.getAs[String]("date") // "YYYY-MM-DD HH:MM:SS"

        if (latStr != null && lonStr != null && dateFull != null) {
          // Arrotondamento alla prima cifra decimale
          val lat = BigDecimal(latStr).setScale(1, RoundingMode.HALF_UP).toDouble
          val lon = BigDecimal(lonStr).setScale(1, RoundingMode.HALF_UP).toDouble

          // Estrazione solo della data (YYYY-MM-DD)
          val date = dateFull.split(" ")(0)

          Some(((lat, lon), date))
        } else {
          None
        }
      } catch {
        case _: Exception => None // Gestione righe malformate
      }
    })

    // Rimuoviamo duplicati causati dall'arrotondamento nella stessa data
    val uniqueEvents = formattedData.distinct()

    // Preparazione per il Join (Map) con
        // Chiave: Data
        // Valore: Location
    // Ripartizioniamo qui per distribuire il carico del join
    val eventsByDate = uniqueEvents
      .map { case (loc, date) => (date, loc) }
      .repartition(numPartitions)

    // Self-Join per trovare Co-occorrenze
    // Uniamo il dataset con se stesso sulla chiave "Data"
    val coOccurrences = eventsByDate.join(eventsByDate)
        // Filtriamo per evitare che una località co-occorra con se stessa evitando anche i duplicati speculari
        .filter { case (_, (loc1, loc2)) =>
        // Confrontiamo Latitudine prima, poi Longitudine
        if (loc1 == loc2) false
        else if (loc1._1 < loc2._1) true
        else if (loc1._1 == loc2._1 && loc1._2 < loc2._2) true
        else false
      }

    // Aggregazione Risultati
    // Vogliamo raggruppare (Date, (Loc1,Loc2)) per coppia di luoghi.
    val pairDates = coOccurrences.map { case (date, pair) => (pair, date) }

    // Raggruppiamo per ottenere la lista delle date per ogni coppia
    val groupedPairs = pairDates.groupByKey()

	// Identificazione del massimo
    // Cerchiamo la coppia con la lista di date più lunga
    if (!groupedPairs.isEmpty()) {
      val (bestPair, datesIterable) = groupedPairs
        .mapValues(dates => {
          val dateList = dates.toList.sorted // Ordine crescente
          (dateList.size, dateList)
        })
        .max()(Ordering.by(_._2._1)) // Confronta in base alla dimensione (count)

      // Output Formattato
      printlnResult(bestPair, datesIterable._2)

    } else {
      println("Nessuna co-occorrenza trovata.")
    }

    spark.stop()
  }

  def printlnResult(pair: (Location, Location), dates: List[String]): Unit = {
    println(s"(${pair._1}, ${pair._2})")
    dates.foreach(println)
  }
}
