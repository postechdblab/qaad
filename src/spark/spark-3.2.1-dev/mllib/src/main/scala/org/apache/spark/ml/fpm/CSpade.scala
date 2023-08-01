package org.apache.spark.ml.fpm

//import org.apache.spark.annotation.Since
import org.apache.spark.ml.param._
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.ml.util.Instrumentation.instrumented
import org.apache.spark.mllib.fpm.{CSpade => mllibCSpade/*, CSpadeModel*/}
import org.apache.spark.sql.{DataFrame, /*Dataset,*/ Row}
//import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.{ArrayType, LongType, IntegerType, StructField, StructType}

class CSpade(override val uid: String) extends Params {
  def this() = this(Identifiable.randomUID("CSpade"))

  val minSupport = new DoubleParam(this, "minSupport", "", ParamValidators.gtEq(-1.0))

  def getMinSupport: Double = $(minSupport)

  def setMinSupport(value: Double): this.type = set(minSupport, value)

  val maxPatternLength = new IntParam(this, "maxPatternLength", "", ParamValidators.gtEq(-1.0))

  def getMaxPatternLength: Int = $(maxPatternLength)

  def setMaxPatternLength(value: Int): this.type = set(maxPatternLength, value)

  val maxWidth = new IntParam(this, "maxWidth", "", ParamValidators.gtEq(-1.0))

  def getMaxWidth: Int = $(maxWidth)

  def setMaxWidth(value: Int): this.type = set(maxWidth, value)

  val minGap = new IntParam(this, "minGap", "", ParamValidators.gtEq(-1.0))

  def getMinGap: Int = $(minGap)

  def setMinGap(value: Int): this.type = set(minGap, value)

  val maxGap = new IntParam(this, "maxGap", "", ParamValidators.gtEq(-1.0))

  def getMaxGap: Int = $(maxGap)

  def setMaxGap(value: Int): this.type = set(maxGap, value)

  val window = new IntParam(this, "window", "", ParamValidators.gtEq(-1.0))

  def getWindow: Int = $(window)

  def setWindow(value: Int): this.type = set(window, value)

  val sequenceCol = new Param[String](this, "sequenceCol", "")

  def getSequenceCol: String = $(sequenceCol)

  def setSequenceCol(value: String): this.type = set(sequenceCol, value)

  val minConfidence = new DoubleParam(this, "minConfidence", "", ParamValidators.gtEq(0.0))

  def getMinConfidence: Double = $(minConfidence)

  def setMinConfidence(value: Double): this.type = set(minConfidence, value)

  val minLift = new DoubleParam(this, "minLift", "", ParamValidators.gtEq(0.0))

  def getMinLift: Double = $(minLift)

  def setMinLift(value: Double): this.type = set(minLift, value)

  val removeRepetitivePattern = new BooleanParam(this, "removeRepetitivePattern", "")

  def getRemoveRepetitivePattern: Boolean = $(removeRepetitivePattern)

  def setRemoveRepetitivePattern(value: Boolean): this.type = set(removeRepetitivePattern, value)

  setDefault(minSupport -> 0.1, maxPatternLength -> -1, maxWidth -> -1, minGap -> -1, maxGap -> -1, window -> -1,  sequenceCol -> "items", minConfidence -> 0.0, minLift -> 0.0, removeRepetitivePattern -> false)

  def findFrequentSequentialPatterns(dataset: DataFrame): DataFrame = instrumented { instr =>
    instr.logDataset(dataset)
    instr.logParams(this, params: _*)

    val sequenceColParam = $(sequenceCol)

    val mllibCSpade = new mllibCSpade()
      .setMinSupport($(minSupport))
      .setMaxPatternLength($(maxPatternLength))
      .setMaxWidth($(maxWidth))
      .setMinGap($(minGap))
      .setMaxGap($(maxGap))
      .setWindow($(window))
      .setMinConfidence($(minConfidence))
      .setMinLift($(minLift))
      .setRemoveRepetitivePattern($(removeRepetitivePattern))

    val rows = mllibCSpade.fit(dataset).freqSequences.map(f => Row(f.sequence, f.freq, f.sids))

    val schema = StructType(Seq(
      StructField("sequence", ArrayType(ArrayType(IntegerType, false), true), nullable = false),
      StructField("freq", LongType, nullable = false),
      StructField("sids", ArrayType(IntegerType), nullable = false)))
    
    val freqSequences = dataset.sparkSession.createDataFrame(rows, schema)

    freqSequences
  }

  override def copy(extra: ParamMap): CSpade = defaultCopy(extra)
}
