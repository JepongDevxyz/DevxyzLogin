package com.devxyz.login.model

/**
 * Full ISO 3166-1 country list — 249 entries, IDENTICAL to the web
 * dashboard v3 COUNTRIES array (data parity between tracks).
 * Picker order mirrors the GoLogin capture + the GoLogin Android app
 * (Play screenshot): France, Germany, United Kingdom, United States,
 * Australia pinned first, then A-Z. Flag emoji are the regional-
 * indicator pair of the alpha-2 code (verified against web v3).
 */
data class Country(val code: String, val name: String)

object Countries {

    val all: List<Country> = listOf(
        Country("FR", "France"), Country("DE", "Germany"), Country("GB", "United Kingdom"),
        Country("US", "United States"), Country("AU", "Australia"), Country("AF", "Afghanistan"),
        Country("AX", "Åland Islands"), Country("AL", "Albania"), Country("DZ", "Algeria"),
        Country("AS", "American Samoa"), Country("AD", "Andorra"), Country("AO", "Angola"),
        Country("AI", "Anguilla"), Country("AQ", "Antarctica"), Country("AG", "Antigua and Barbuda"),
        Country("AR", "Argentina"), Country("AM", "Armenia"), Country("AW", "Aruba"),
        Country("AT", "Austria"), Country("AZ", "Azerbaijan"), Country("BS", "Bahamas"),
        Country("BH", "Bahrain"), Country("BD", "Bangladesh"), Country("BB", "Barbados"),
        Country("BY", "Belarus"), Country("BE", "Belgium"), Country("BZ", "Belize"),
        Country("BJ", "Benin"), Country("BM", "Bermuda"), Country("BT", "Bhutan"),
        Country("BO", "Bolivia"), Country("BQ", "Bonaire, Sint Eustatius and Saba"), Country("BA", "Bosnia and Herzegovina"),
        Country("BW", "Botswana"), Country("BV", "Bouvet Island"), Country("BR", "Brazil"),
        Country("IO", "British Indian Ocean Territory"), Country("BN", "Brunei"), Country("BG", "Bulgaria"),
        Country("BF", "Burkina Faso"), Country("BI", "Burundi"), Country("CV", "Cabo Verde"),
        Country("KH", "Cambodia"), Country("CM", "Cameroon"), Country("CA", "Canada"),
        Country("KY", "Cayman Islands"), Country("CF", "Central African Republic"), Country("TD", "Chad"),
        Country("CL", "Chile"), Country("CN", "China"), Country("CX", "Christmas Island"),
        Country("CC", "Cocos (Keeling) Islands"), Country("CO", "Colombia"), Country("KM", "Comoros"),
        Country("CG", "Congo"), Country("CD", "Congo (DRC)"), Country("CK", "Cook Islands"),
        Country("CR", "Costa Rica"), Country("CI", "Côte d'Ivoire"), Country("HR", "Croatia"),
        Country("CU", "Cuba"), Country("CW", "Curaçao"), Country("CY", "Cyprus"),
        Country("CZ", "Czechia"), Country("DK", "Denmark"), Country("DJ", "Djibouti"),
        Country("DM", "Dominica"), Country("DO", "Dominican Republic"), Country("EC", "Ecuador"),
        Country("EG", "Egypt"), Country("SV", "El Salvador"), Country("GQ", "Equatorial Guinea"),
        Country("ER", "Eritrea"), Country("EE", "Estonia"), Country("SZ", "Eswatini"),
        Country("ET", "Ethiopia"), Country("FK", "Falkland Islands"), Country("FO", "Faroe Islands"),
        Country("FJ", "Fiji"), Country("FI", "Finland"), Country("GF", "French Guiana"),
        Country("PF", "French Polynesia"), Country("TF", "French Southern Territories"), Country("GA", "Gabon"),
        Country("GM", "Gambia"), Country("GE", "Georgia"), Country("GH", "Ghana"),
        Country("GI", "Gibraltar"), Country("GR", "Greece"), Country("GL", "Greenland"),
        Country("GD", "Grenada"), Country("GP", "Guadeloupe"), Country("GU", "Guam"),
        Country("GT", "Guatemala"), Country("GG", "Guernsey"), Country("GN", "Guinea"),
        Country("GW", "Guinea-Bissau"), Country("GY", "Guyana"), Country("HT", "Haiti"),
        Country("HM", "Heard Island and McDonald Islands"), Country("VA", "Holy See"), Country("HN", "Honduras"),
        Country("HK", "Hong Kong"), Country("HU", "Hungary"), Country("IS", "Iceland"),
        Country("IN", "India"), Country("ID", "Indonesia"), Country("IR", "Iran"),
        Country("IQ", "Iraq"), Country("IE", "Ireland"), Country("IM", "Isle of Man"),
        Country("IL", "Israel"), Country("IT", "Italy"), Country("JM", "Jamaica"),
        Country("JP", "Japan"), Country("JE", "Jersey"), Country("JO", "Jordan"),
        Country("KZ", "Kazakhstan"), Country("KE", "Kenya"), Country("KI", "Kiribati"),
        Country("KP", "North Korea"), Country("KR", "South Korea"), Country("XK", "Kosovo"),
        Country("KW", "Kuwait"), Country("KG", "Kyrgyzstan"), Country("LA", "Laos"),
        Country("LV", "Latvia"), Country("LB", "Lebanon"), Country("LS", "Lesotho"),
        Country("LR", "Liberia"), Country("LY", "Libya"), Country("LI", "Liechtenstein"),
        Country("LT", "Lithuania"), Country("LU", "Luxembourg"), Country("MO", "Macao"),
        Country("MG", "Madagascar"), Country("MW", "Malawi"), Country("MY", "Malaysia"),
        Country("MV", "Maldives"), Country("ML", "Mali"), Country("MT", "Malta"),
        Country("MH", "Marshall Islands"), Country("MQ", "Martinique"), Country("MR", "Mauritania"),
        Country("MU", "Mauritius"), Country("YT", "Mayotte"), Country("MX", "Mexico"),
        Country("FM", "Micronesia"), Country("MD", "Moldova"), Country("MC", "Monaco"),
        Country("MN", "Mongolia"), Country("ME", "Montenegro"), Country("MS", "Montserrat"),
        Country("MA", "Morocco"), Country("MZ", "Mozambique"), Country("MM", "Myanmar"),
        Country("NA", "Namibia"), Country("NR", "Nauru"), Country("NP", "Nepal"),
        Country("NL", "Netherlands"), Country("NC", "New Caledonia"), Country("NZ", "New Zealand"),
        Country("NI", "Nicaragua"), Country("NE", "Niger"), Country("NG", "Nigeria"),
        Country("NU", "Niue"), Country("NF", "Norfolk Island"), Country("MK", "North Macedonia"),
        Country("MP", "Northern Mariana Islands"), Country("NO", "Norway"), Country("OM", "Oman"),
        Country("PK", "Pakistan"), Country("PW", "Palau"), Country("PS", "Palestine"),
        Country("PA", "Panama"), Country("PG", "Papua New Guinea"), Country("PY", "Paraguay"),
        Country("PE", "Peru"), Country("PH", "Philippines"), Country("PN", "Pitcairn"),
        Country("PL", "Poland"), Country("PT", "Portugal"), Country("PR", "Puerto Rico"),
        Country("QA", "Qatar"), Country("RE", "Réunion"), Country("RO", "Romania"),
        Country("RU", "Russia"), Country("RW", "Rwanda"), Country("BL", "Saint Barthélemy"),
        Country("SH", "Saint Helena"), Country("KN", "Saint Kitts and Nevis"), Country("LC", "Saint Lucia"),
        Country("MF", "Saint Martin"), Country("PM", "Saint Pierre and Miquelon"), Country("VC", "Saint Vincent and the Grenadines"),
        Country("WS", "Samoa"), Country("SM", "San Marino"), Country("ST", "Sao Tome and Principe"),
        Country("SA", "Saudi Arabia"), Country("SN", "Senegal"), Country("RS", "Serbia"),
        Country("SC", "Seychelles"), Country("SL", "Sierra Leone"), Country("SG", "Singapore"),
        Country("SX", "Sint Maarten"), Country("SK", "Slovakia"), Country("SI", "Slovenia"),
        Country("SB", "Solomon Islands"), Country("SO", "Somalia"), Country("ZA", "South Africa"),
        Country("GS", "South Georgia and the South Sandwich Islands"), Country("SS", "South Sudan"), Country("ES", "Spain"),
        Country("LK", "Sri Lanka"), Country("SD", "Sudan"), Country("SR", "Suriname"),
        Country("SJ", "Svalbard and Jan Mayen"), Country("SE", "Sweden"), Country("CH", "Switzerland"),
        Country("SY", "Syria"), Country("TW", "Taiwan"), Country("TJ", "Tajikistan"),
        Country("TZ", "Tanzania"), Country("TH", "Thailand"), Country("TL", "Timor-Leste"),
        Country("TG", "Togo"), Country("TK", "Tokelau"), Country("TO", "Tonga"),
        Country("TT", "Trinidad and Tobago"), Country("TN", "Tunisia"), Country("TR", "Turkey"),
        Country("TM", "Turkmenistan"), Country("TC", "Turks and Caicos Islands"), Country("TV", "Tuvalu"),
        Country("UG", "Uganda"), Country("UA", "Ukraine"), Country("AE", "United Arab Emirates"),
        Country("UM", "United States Minor Outlying Islands"), Country("UY", "Uruguay"), Country("UZ", "Uzbekistan"),
        Country("VU", "Vanuatu"), Country("VE", "Venezuela"), Country("VN", "Vietnam"),
        Country("VG", "Virgin Islands (British)"), Country("VI", "Virgin Islands (U.S.)"), Country("EH", "Western Sahara"),
        Country("YE", "Yemen"), Country("ZM", "Zambia"), Country("ZW", "Zimbabwe")
    )

    fun byCode(code: String): Country =
        all.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Country("US", "United States")

    fun nameOf(code: String): String = byCode(code).name

    /** Regional-indicator flag emoji for an alpha-2 code ("" when not A-Z x2). */
    fun flag(code: String): String {
        val c = code.uppercase()
        if (c.length != 2 || !c.all { it in 'A'..'Z' }) return ""
        return c.map { ch -> String(Character.toChars(0x1F1E6 + (ch - 'A'))) }.joinToString("")
    }
}
