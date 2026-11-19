package co.com.bdb.automation.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Diseno del archivo RESDWI.TXT.
 * Requerimiento: CVAPD00685673 | Jira: SP-2986 | Componente: CICS TS (Subfuente 337)
 * Control SQL*Loader: ODS_IDIA_ATTXN.ctl (73 campos oficiales; hueco sin mapear en 500:561; total 001:819)
 * Trailer (WHEN (1:5) = 'CNTRL'): DET_FEC_ORIGEN (006:013), DET_NM_ARCHIVO (014:039), DET_VALOR_ORIGEN (040:059)
 */
public final class DefinicionResdwi {

    public static final int LONGITUD_ESPERADA = 819;
    public static final String SUBFUENTE_ESPERADA = "337";
    public static final String TRAILER_ID = "CNTRL";
    public static final int[] TRAILER_FECHA = {6, 13};
    public static final int[] TRAILER_NOMBRE = {14, 39};
    public static final int[] TRAILER_VALOR = {40, 59};
    public static final String NOMBRE_ARCHIVO_ESPERADO = "RESDWI.TXT";
    public static final String CAMPO_SUMA_MONTO = "ATTXN_VAL_TRANSAC";

    // Hueco sin mapear en ODS_IDIA_ATTXN.ctl (bytes 500 a 561 = 62 bytes)
    public static final int HUECO_INICIO = 500;
    public static final int HUECO_FIN = 561;
    public static final int HUECO_LONGITUD = 62;

    public static final Set<String> FECHAS_YYYYMMDD = Set.of(
            "ATTXN_FEC_CORTE", "ATTXN_FEC_BACKDAY", "ATTXN_FEC_REAL", "ATTXN_FEC_CONTAB");

    public static final Set<String> NUMERICOS_OBLIGATORIOS = Set.of(
            "ATTXN_VAL_EFECTIVO", "ATTXN_VAL_CHEQUE", "ATTXN_VAL_CARG_CTA", "ATTXN_VAL_CARGO4",
            "ATTXN_VAL_CARGO5", "ATTXN_VAL_TRANSAC", "ATTXN_VALOR_IVA_TS", "ATTXN_BASE_IVA");

    /** Numericos que admiten NULL (DECODE) con su divisor. */
    public static final Map<String, Double> NUMERICOS_CON_NULL;

    public static final Set<String> ENTEROS_EXTERNOS = Set.of(
            "ATTXN_BYTE_ESTADO", "ATTXN_IND_NEXTDAY", "ATTXN_CAN_CHEQ");

    static {
        Map<String, Double> m = new HashMap<>();
        m.put("ATTXN_BASE_GRAV", 100.0);
        m.put("ATTXN_TAR_GMF", 1000.0);
        NUMERICOS_CON_NULL = Collections.unmodifiableMap(m);
    }

    public static final List<CampoCtl> CAMPOS = List.of(
            new CampoCtl(1, "ATTXN_SUB_FUENTE", 1, 3, 3, "CHAR", true),
            new CampoCtl(2, "ATTXN_SECUENCIA", 4, 23, 20, "TRIM(:ATTXN_SECUENCIA)", true),
            new CampoCtl(3, "ATTXN_SEC_CORRE", 24, 43, 20, "TRIM(:ATTXN_SEC_CORRE)", false),
            new CampoCtl(4, "ATTXN_NIVEL1_TS", 44, 47, 4, "CHAR", true),
            new CampoCtl(5, "ATTXN_COD_TRANSAC", 48, 51, 4, "CHAR", true),
            new CampoCtl(6, "ATTXN_CANAL", 52, 53, 2, "CHAR", true),
            new CampoCtl(7, "ATTXN_RED", 54, 57, 4, "CHAR", true),
            new CampoCtl(8, "ATTXN_TIP_DISP", 58, 60, 3, "CHAR", false),
            new CampoCtl(9, "ATTXN_COD_TERMINAL", 61, 68, 8, "CHAR", false),
            new CampoCtl(10, "ATTXN_COD_CAJERO", 69, 76, 8, "CHAR", false),
            new CampoCtl(11, "ATTXN_BYTE_ESTADO", 77, 77, 1, "INTEGER EXTERNAL", true),
            new CampoCtl(12, "ATTXN_IND_NEXTDAY", 78, 78, 1, "INTEGER EXTERNAL", false),
            new CampoCtl(13, "ATTXN_IND_REVERSI", 79, 79, 1, "DECODE(TRIM(:ATTXN_IND_REVERSI),'1','1','0')", true),
            new CampoCtl(14, "ATTXN_IND_DOB_REV", 80, 80, 1, "DECODE(TRIM(:ATTXN_IND_DOB_REV),'1','1','0')", true),
            new CampoCtl(15, "ATTXN_SEC_TS_ORIG", 81, 84, 4, "CHAR", false),
            new CampoCtl(16, "ATTXN_SEC_TS_REV", 85, 88, 4, "CHAR", false),
            new CampoCtl(17, "ATTXN_TERMINAL_TS", 89, 96, 8, "CHAR", false),
            new CampoCtl(18, "ATTXN_FEC_CORTE", 97, 104, 8, "TO_DATE(rtrim(:ATTXN_FEC_CORTE), 'YYYYMMDD')", true),
            new CampoCtl(19, "ATTXN_PERIODO", 97, 102, 6, "CHAR", false),
            new CampoCtl(20, "ATTXN_DIA", 103, 104, 2, "CHAR", false),
            new CampoCtl(21, "ATTXN_FEC_BACKDAY", 105, 112, 8, "TO_DATE(rtrim(:ATTXN_FEC_BACKDAY), 'YYYYMMDD')", false),
            new CampoCtl(22, "ATTXN_FEC_REAL", 113, 120, 8, "TO_DATE(rtrim(:ATTXN_FEC_REAL), 'YYYYMMDD')", true),
            new CampoCtl(23, "ATTXN_FEC_CONTAB", 121, 128, 8, "TO_DATE(rtrim(:ATTXN_FEC_CONTAB), 'YYYYMMDD')", true),
            new CampoCtl(24, "ATTXN_HORA", 129, 134, 6, "CHAR", true),
            new CampoCtl(25, "ATTXN_OFI_RECEP", 135, 138, 4, "CHAR", false),
            new CampoCtl(26, "ATTXN_TIP_MVTO", 139, 140, 2, "CHAR", false),
            new CampoCtl(27, "ATTXN_TIP_TRAN_TS", 141, 142, 2, "CHAR", false),
            new CampoCtl(28, "ATTXN_TIP_TRANSAC", 141, 142, 2, "CHAR", false),
            new CampoCtl(29, "ATTXN_TIP_CTA_AFOR", 143, 144, 2, "CHAR", true),
            new CampoCtl(30, "ATTXN_NUM_CTA_AFOR", 145, 163, 19, "TRIM(:ATTXN_NUM_CTA_AFOR)", true),
            new CampoCtl(31, "ATTXN_NUM_CTA_AFD9", 155, 163, 9, "CHAR", false),
            new CampoCtl(32, "ATTXN_OFI_CTA_AFOR", 164, 167, 4, "DECODE(:ATTXN_OFI_CTA_AFOR,'0001','0000',:ATTXN_OFI_CTA_AFOR)", false),
            new CampoCtl(33, "ATTXN_COD_COMPEN", 168, 170, 3, "CHAR", false),
            new CampoCtl(34, "ATTXN_TIP_IDEN_CLI", 171, 172, 2, "CHAR", false),
            new CampoCtl(35, "ATTXN_NUM_IDEN_CLI", 173, 183, 11, "CHAR", false),
            new CampoCtl(36, "ATTXN_TIP_OT_CTA", 184, 185, 2, "CHAR", false),
            new CampoCtl(37, "ATTXN_NUM_OT_CTA", 186, 204, 19, "CHAR", false),
            new CampoCtl(38, "ATTXN_OFI_CTA_DEST", 205, 208, 4, "DECODE(:ATTXN_OFI_CTA_DEST,'0001','0000',:ATTXN_OFI_CTA_DEST)", false),
            new CampoCtl(39, "ATTXN_COD_COM_DEST", 209, 211, 3, "CHAR", false),
            new CampoCtl(40, "ATTXN_FORMA_PAGO", 212, 213, 2, "CHAR", false),
            new CampoCtl(41, "ATTXN_NUM_DOCU", 214, 223, 10, "CHAR", false),
            new CampoCtl(42, "ATTXN_VAL_EFECTIVO", 224, 241, 18, "(:ATTXN_VAL_EFECTIVO / 100 )", true),
            new CampoCtl(43, "ATTXN_VAL_CHEQUE", 242, 259, 18, "(:ATTXN_VAL_CHEQUE / 100 )", true),
            new CampoCtl(44, "ATTXN_VAL_CARG_CTA", 260, 277, 18, "(:ATTXN_VAL_CARG_CTA / 100 )", true),
            new CampoCtl(45, "ATTXN_TIP_CARGO4", 278, 280, 3, "CHAR", false),
            new CampoCtl(46, "ATTXN_VAL_CARGO4", 281, 298, 18, "(:ATTXN_VAL_CARGO4 / 100 )", true),
            new CampoCtl(47, "ATTXN_TIP_CARGO5", 299, 301, 3, "CHAR", false),
            new CampoCtl(48, "ATTXN_VAL_CARGO5", 302, 319, 18, "(:ATTXN_VAL_CARGO5 / 100 )", true),
            new CampoCtl(49, "ATTXN_VAL_TRANSAC", 320, 337, 18, "(:ATTXN_VAL_TRANSAC / 100 )", true),
            new CampoCtl(50, "ATTXN_REF_CHEQ", 338, 342, 5, "CHAR", false),
            new CampoCtl(51, "ATTXN_CAN_CHEQ", 343, 347, 5, "INTEGER EXTERNAL", false),
            new CampoCtl(52, "ATTXN_COD_DANE", 348, 352, 5, "CHAR", false),
            new CampoCtl(53, "ATTXN_COD_SEVERID", 353, 353, 1, "CHAR", false),
            new CampoCtl(54, "ATTXN_COD_RECHAZO", 354, 359, 6, "CHAR", false),
            new CampoCtl(55, "ATTXN_DES_RECHAZO", 360, 409, 50, "TRIM(:ATTXN_DES_RECHAZO)", false),
            new CampoCtl(56, "ATTXN_REFERENCIA1", 410, 439, 30, "TRIM(:ATTXN_REFERENCIA1)", false),
            new CampoCtl(57, "ATTXN_REFERENCIA2", 440, 469, 30, "TRIM(:ATTXN_REFERENCIA2)", false),
            new CampoCtl(58, "ATTXN_REFERENCIA3", 470, 499, 30, "TRIM(:ATTXN_REFERENCIA3)", false),
            new CampoCtl(59, "ATTXN_NUM_IDE_ORDENAN", 562, 576, 15, "TRIM(:ATTXN_NUM_IDE_ORDENAN)", false),
            new CampoCtl(60, "ATTXN_NOM_GESTOR", 577, 636, 60, "CHAR", false),
            new CampoCtl(61, "ATTXN_TIP_IDE_GESTOR", 637, 638, 2, "CHAR", false),
            new CampoCtl(62, "ATTXN_NUM_IDE_GESTOR", 639, 653, 15, "CHAR", false),
            new CampoCtl(63, "ATTXN_CICLO", 654, 655, 2, "CHAR", false),
            new CampoCtl(64, "ATTXN_PLANILLA_CUS", 656, 685, 30, "ltrim(trim(:ATTXN_PLANILLA_CUS),'0')", false),
            new CampoCtl(65, "ATTXN_BASE_GRAV", 686, 703, 18, "DECODE(:ATTXN_BASE_GRAV,NULL,NULL,to_number(:ATTXN_BASE_GRAV ) / 100)", false),
            new CampoCtl(66, "ATTXN_TAR_GMF", 704, 709, 6, "DECODE(:ATTXN_TAR_GMF,NULL,NULL,to_number(:ATTXN_TAR_GMF) / 1000)", false),
            new CampoCtl(67, "ATTXN_FECHA_ORIGEN", 710, 717, 8, "to_date(DECODE(:ATTXN_FECHA_ORIGEN,NULL,NULL,:ATTXN_FECHA_ORIGEN),'MMDDYY')", false),
            new CampoCtl(68, "ATTXN_HORA_ORIGEN", 718, 725, 8, "CHAR", false),
            new CampoCtl(69, "ATTXN_AYUDA", 726, 755, 30, "CHAR", false),
            new CampoCtl(70, "ATTXN_FLUJO", 756, 775, 20, "CHAR", false),
            new CampoCtl(71, "ATTXN_VALOR_IVA_TS", 776, 793, 18, "(:ATTXN_VALOR_IVA_TS / 100 )", true),
            new CampoCtl(72, "ATTXN_BASE_IVA", 794, 811, 18, "(:ATTXN_BASE_IVA / 100 )", true),
            new CampoCtl(73, "ATTXN_COD_AUTORIZA", 812, 819, 8, "CHAR", false)
    );

    private DefinicionResdwi() {
    }

    public static CampoCtl campo(String nombre) {
        return CAMPOS.stream().filter(c -> c.getNombre().equalsIgnoreCase(nombre)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Campo no definido en el .ctl: " + nombre));
    }
}