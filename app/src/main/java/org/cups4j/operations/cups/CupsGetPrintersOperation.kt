package org.cups4j.operations.cups

/**
 * Copyright (C) 2009 Harald Weyhing
 *
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *
 * See the GNU Lesser General Public License for more details. You should have received a copy of
 * the GNU Lesser General Public License along with this program; if not, see
 * <http:></http:>//www.gnu.org/licenses/>.
 */

/*Notice
 * This file has been modified. It is not the original.
 * Jon Freeman - 2013
 */

import io.github.benoitduffez.cupsprint.L
import org.cups4j.CupsPrinter
import org.cups4j.operations.IppOperation
import java.net.URL
import java.util.ArrayList
import java.util.HashMap

class CupsGetPrintersOperation : IppOperation() {
    init {
        operationID = 0x4002
        bufferSize = 8192
    }

    @Throws(Exception::class)
    fun getPrinters(url: URL, path: String): List<CupsPrinter> {
        val printers = ArrayList<CupsPrinter>()

        val map = HashMap<String, String>()
        map["requested-attributes"] = "copies-supported page-ranges-supported printer-name printer-info printer-location printer-make-and-model printer-uri-supported"

        val result = request(URL(url.toString() + path), map)

        if (result == null) {
            L.e("Couldn't get printers from URL: $url with path: $path")
            return printers
        }

        for (group in result.attributeGroupList!!) {
            val printer: CupsPrinter
            if (group.tagName == "printer-attributes-tag") {
                var printerURI: String? = null
                var printerName: String? = null
                var printerLocation: String? = null
                var printerDescription: String? = null
                for (attr in group.attribute) {
                    when (attr.name) {
                        "printer-uri-supported" -> printerURI = attr.attributeValue[0].value!!.replace("ipps?://".toRegex(), url.protocol + "://")
                        "printer-name" -> printerName = attr.attributeValue[0].value
                        "printer-location" -> if (attr.attributeValue.size > 0) {
                            printerLocation = attr.attributeValue[0].value
                        }
                        "printer-info" -> if (attr.attributeValue.size > 0) {
                            printerDescription = attr.attributeValue[0].value
                        }
                    }
                }
                val printerUrl: URL
                try {
                    printerUrl = URL(printerURI)
                } catch (t: Throwable) {
                    t.printStackTrace()
                    System.err.println("Error encountered building URL from printer uri of printer " + printerName
                            + ", uri returned was [" + printerURI + "].  Attribute group tag/description: [" + group.tagName
                            + "/" + group.description)
                    throw Exception(t)
                }

                printer = CupsPrinter(printerUrl, printerName ?: DEFAULT_PRINTER_NAME, false)
                printer.location = printerLocation
                printer.description = printerDescription
                printers.add(printer)
            }
        }

        return printers
    }
}