import org.msgpack.core.MessagePack
import org.msgpack.value.ImmutableValue
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.StringBuilder
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        showHelp()
        exitProcess(0)
    }
    val unpacker = MessagePack.newDefaultUnpacker(FileInputStream(args[0]));
    val reportContentBuilder = StringBuilder()
    var dataPosition = 0
    val simpleDateFragment = SimpleDateFormat("YYYY年MM月dd日 HH:mm:ss", Locale.getDefault())

    var storeTime = 0L
    while (unpacker.hasNext()) {
        val value = unpacker.unpackValue()
        when (dataPosition) {
            0 -> reportContentBuilder.append("缓存头大小：${value}字节\n")
            1 -> reportContentBuilder.append("缓存版本号：${value}\n")
            2 -> {
                if (value.isIntegerValue) {
                    when (value.asIntegerValue().toInt()) {
                        0 -> reportContentBuilder.append("储存类型：STORE_TYPE_UNKNOWN\n")
                        -1 -> reportContentBuilder.append("储存类型：STORE_TYPE_CRASH\n")
                        1 -> reportContentBuilder.append("储存类型：STORE_TYPE_AUTO\n")
                    }
                }
            }
            3 -> reportContentBuilder.append("TAG：${value}\n")
            4 -> {
                storeTime = value.asIntegerValue().toLong()
                reportContentBuilder.append("创建时间：").append(simpleDateFragment.format(Date(storeTime))).append("\n")
            }
            5 -> {
                val effectiveDuration = value.asIntegerValue().toLong()
                if (effectiveDuration > 0) {
                    reportContentBuilder.append("过期时间：")
                        .append(simpleDateFragment.format(Date(storeTime + effectiveDuration))).append("\n")
                } else {
                    reportContentBuilder.append("永久有效\n")
                }
            }
            else -> {
                reportContentBuilder.append(genValueWithType(value)).append("\n")
            }
        }
        dataPosition++
    }
    var reportFileName = "report.txt"
    if (args.size >= 2) reportFileName = args[1]
    val output = FileOutputStream(reportFileName)
    output.write(reportContentBuilder.toString().toByteArray(Charset.forName("UTF-8")))

}

fun genValueWithType(value: ImmutableValue): String {
    if (value.isNilValue) return "[null]     |  "
    if (value.isBooleanValue) return "[Boolean]  |  " + value.asBooleanValue()
    if (value.isIntegerValue) return "[Integer]  |  " + value.asIntegerValue()
    if (value.isFloatValue) return "[Float]    |  " + value.asFloatValue()
    if (value.isStringValue) return "[String]   |  " + value.asStringValue()
    if (value.isArrayValue) return "[Array]    |  " + value.asArrayValue()
    if (value.isMapValue) return "[Map]      |  " + value.asMapValue()
    if (value.isNumberValue) return "[Number]   |  " + value.asNumberValue()
    if (value.isBinaryValue) return "[HEX]      |  " + value.asBooleanValue()
    if (value.isRawValue) return "[RAW]      |  " + value.asRawValue()

    return "[unknown]"
}

fun showHelp() {
    println("用法 java -jar MessagePackDataPreviewer.jar 文件名 [报告文件名]")
}