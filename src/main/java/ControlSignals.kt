
enum class ControlSignal(val value: Long, val inverted: Boolean = true) {
    ALS0(0x01, false),
    ALS1(0x02, false),
    ALS2(0x04, false),
    ALO(0x08),
    ALW(0x10),
    ALB(0x20),
    ALC(0x40, false),

    PCHO(0x0100),
    PCHI(0x0200),
    PCLO(0x0400),
    PCLI(0x0800),
    PCC(0x1000, false),
    FO(0x2000),
    FW(0x4000),
    FI(0x8000),

    AO(0x010000),
    AI(0x020000),
    BO(0x040000),
    BI(0x080000),
    CO(0x100000),
    CI(0x200000),
    SPO(0x400000),
    SPI(0x800000),

    IRI(0x08000000),
    INO(0x10000000),
    SI(0x20000000),
    DI(0x40000000, false),
    DRS(0x80000000, false),

    HLT(0x0100000000),
    TR(0x0200000000),
    MAHI(0x0400000000),
    MALI(0x0800000000),
    ME(0x1000000000),
    MW(0x2000000000),
    MS(0x4000000000, false),
    PGO(0x8000000000, false);
}

val ALS0 = ControlSignal.ALS0
val ALS1 = ControlSignal.ALS1
val ALS2 = ControlSignal.ALS2
val ALO = ControlSignal.ALO
val ALW = ControlSignal.ALW
val ALB = ControlSignal.ALB
val ALC = ControlSignal.ALC

val PCHO = ControlSignal.PCHO
val PCHI = ControlSignal.PCHI
val PCLO = ControlSignal.PCLO
val PCLI = ControlSignal.PCLI
val PCC = ControlSignal.PCC
val FO = ControlSignal.FO
val FW = ControlSignal.FW
val FI = ControlSignal.FI

val AO = ControlSignal.AO
val AI = ControlSignal.AI
val BO = ControlSignal.BO
val BI = ControlSignal.BI
val CO = ControlSignal.CO
val CI = ControlSignal.CI
val SPO = ControlSignal.SPO
val SPI = ControlSignal.SPI

val IRI = ControlSignal.IRI
val INO = ControlSignal.INO
val SI = ControlSignal.SI
val DI = ControlSignal.DI
val DRS = ControlSignal.DRS

val HLT = ControlSignal.HLT
val TR = ControlSignal.TR
val MAHI = ControlSignal.MAHI
val MALI = ControlSignal.MALI
val ME = ControlSignal.ME
val MW = ControlSignal.MW
val MS = ControlSignal.MS
val PGO = ControlSignal.PGO