package de.kirchheim.handball

/**
 * Testdaten im Format der echten Handball4All-Antworten. Vereinsnamen wie in der Liga,
 * Spielernamen frei erfunden (keine echten Personendaten im Repository).
 */
object Fixtures {
    const val LEAGUE_JSON = """
[{
  "head": {
    "name": "Männer Bezirksklasse Staffel 2",
    "headline1": "Rhein-Neckar",
    "headline2": "Männer Bezirksklasse Staffel 2 - Hallenrunde 2026/2027",
    "actualized": "Letzte Änderung: So, 08.11.2026, 15:25h",
    "repURL": "https://example.invalid/report?sGID="
  },
  "content": {
    "score": [
      {"tabScore": 1, "tabTeamname": "SG Heidelberg-Leimen  2", "numPlayedGames": 5, "numWonGames": 5,
       "numEqualGames": 0, "numLostGames": 0, "numGoalsShot": 152, "numGoalsGot": 115, "pointsPlus": 10, "pointsMinus": 0},
      {"tabScore": 3, "tabTeamname": "SG HD-Kirchheim", "numPlayedGames": "4", "numWonGames": 2,
       "numEqualGames": 0, "numLostGames": 2, "numGoalsShot": 101, "numGoalsGot": 98, "pointsPlus": 4, "pointsMinus": 4},
      {"tabScore": 4, "tabTeamname": "HSG Dielheim/Malschenberg      2", "numPlayedGames": null}
    ],
    "futureGames": {"games": [
      {"gNo": "808494", "sGID": 0, "gDate": "21.11.26", "gTime": "18:00", "gGymnasiumNo": "22017",
       "gGymnasiumName": "Sportzentrum Süd Halle 1", "gHomeTeam": "SG HDKirchheim", "gGuestTeam": "TSG Wiesloch 2",
       "gHomeGoals": " ", "gGuestGoals": " ", "gComment": " "}
    ]},
    "actualGames": [
      {"games": [
        {"gNo": "808452", "sGID": "3501606", "gDate": "19.09.26", "gTime": "20:00", "gGymnasiumNo": "22017",
         "gGymnasiumName": "Sportzentrum Süd Halle 1", "gHomeTeam": "SG HDKirchheim", "gGuestTeam": "TC Kurpfalz",
         "gHomeGoals": "21", "gGuestGoals": "24", "gComment": " "},
        {"gNo": "808467", "sGID": "3523636", "gDate": "03.10.26", "gTime": "17:00", "gGymnasiumNo": "",
         "gGymnasiumName": "", "gHomeTeam": "HSG DiMa 2", "gGuestTeam": "HSV Hockenheim 2",
         "gHomeGoals": "0", "gGuestGoals": "0", "gComment": "(2:0), gg. Gast"}
      ]},
      {"games": [
        {"gNo": "808537", "sGID": 0, "gDate": "10.10.26", "gTime": "16:00", "gHomeTeam": "SG HDKirchheim",
         "gGuestTeam": "HSG DiMa 2", "gHomeGoals": "0", "gGuestGoals": "0", "gComment": "(2:0), gg. Gast, Absage durch HSG DiMa"},
        {"gNo": "808475", "sGID": "3508286", "gDate": "26.09.26", "gTime": "19:30", "gHomeTeam": "TB Neckarsteinach",
         "gGuestTeam": "SG HDKirchheim", "gHomeGoals": "28", "gGuestGoals": "28", "gComment": " "},
        {"gNo": "999999", "sGID": "1", "gDate": "kein Datum", "gHomeTeam": "A", "gGuestTeam": "B"},
        {"sGID": "2", "gDate": "01.01.27", "gHomeTeam": "ohne Nummer", "gGuestTeam": "B"}
      ]}
    ]
  }
}]
"""

    /** Text eines Spielberichts, wie PDFBox/PDFKit ihn liefern (gekürzt, Namen erfunden). */
    val REPORT_LINES = """
Presseinformation - lang
M-BK-2-RN, Spiel Nr. 808452 am 19.09.26
Endstand 4:3 (2:1) , Sieger SG HD-Kirchheim Zuschauer: k.A.
Spielverlauf
Zeit Spielzeit Spielstand Aktion
20:03:58 02:47 1:0 Tor durch Max Muster (13, SG HDKirchheim)
20:06:26 05:16 1:1 Tor durch
20:07:10 06:01 1:2 7m-Tor durch Erika Beispiel (9, TC Kurpfalz)
20:08:00 06:50 7m, KEIN Tor durch Erika Beispiel (9, TC Kurpfalz)
20:18:35 16:54 Verwarnung für Max Muster (13, SG HDKirchheim)
20:20:30 18:48 2:2 7m-Tor durch Max Muster (13, SG HDKirchheim)
Handball4all AG Seite 3/4 Stand vom 09.11.2026, 14:30:35h
Presseinformation - lang
20:20:30 18:48 2:2 7m-Tor durch Max Muster (13, SG HDKirchheim)
20:30:41   27:39   3:2   Tor durch   Paul   Probe (7, SG HDKirchheim)
21:10:43 54:29 Auszeit TC Kurpfalz St.Ilgen
21:12:31 54:42 3:3 Tor durch Erika Beispiel (9, TC Kurpfalz)
21:13:52 56:04 2-min Strafe für
21:15:00 57:00 4:3 Tor durch Max Muster (13, SG HDKirchheim)
""".trimIndent().lines()

    /** Bericht ohne Spielverlauf (z. B. Spiel ohne elektronischen Spielbericht). */
    val REPORT_WITHOUT_GOALS = listOf("Presseinformation - lang", "Spielverlauf", "Zeit Spielzeit Spielstand Aktion")
}
