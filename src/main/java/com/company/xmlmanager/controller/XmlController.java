package com.company.xmlmanager.controller;

import com.company.xmlmanager.service.XmlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;

@Controller
public class XmlController {

    @Autowired
    private XmlService xmlService;

    @GetMapping("/")
    public String formulario() {
        return "index";
    }

    @GetMapping("/version1")
    @ResponseBody
    public String version1() {
        try {
            xmlService.copiarBloqueXml(
                    "src/main/resources/input/20606316225-31-EG03-2.xml",
                    "src/main/resources/input/20535152404-31-VV01-0000003.XML"
            );
            return "salio okey";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/version2")
    public String version2(@RequestParam String documento, RedirectAttributes redirectAttributes) {
        try {
            xmlService.insertarDocumentoAdicional(
                    "src/main/resources/input/20606316225-31-EG03-2.xml",
                    "src/main/resources/input/20535152404-31-VV01-0000003.XML",
                    documento
            );
            redirectAttributes.addFlashAttribute("mensaje", "Insertado correctamente con ID: " + documento);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/version3")
    public String version3(@RequestParam String documento,
                           @RequestParam("archivoXml") MultipartFile archivoXml,
                           RedirectAttributes redirectAttributes) {
        try {
            File tempInput = File.createTempFile("temp-xml", ".xml");
            archivoXml.transferTo(tempInput);
            xmlService.insertarDocumentoAdicional(
                    "src/main/resources/input/20606316225-31-EG03-2.xml",
                    tempInput.getAbsolutePath(),
                    documento
            );
            redirectAttributes.addFlashAttribute("mensaje", "Archivo procesado: " + archivoXml.getOriginalFilename());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error: " + e.getMessage());
        }
        return "redirect:/formulario";
    }

    @GetMapping("/formulario")
    public String formulario3(Model model) {
        return "formulario";
    }

    @PostMapping("/version-final-directo")
    public String editarArchivoEnRutaReal(
            @RequestParam("documento") String nuevoID,
            @RequestParam("rutaDestino") String rutaDestino,
            @RequestParam("otrosDatosConductor") String otrosDatos,
            RedirectAttributes redirectAttributes
    ) {
        try {
            File archivoFuente = new File("src/main/resources/input/20606316225-31-EG03-2.xml");
            File archivoReal = new File(rutaDestino);

            if (!archivoReal.exists()) {
                throw new Exception("El archivo no existe: " + rutaDestino);
            }

            // Aquí pasas también "otrosDatos"
            xmlService.copiarDesdeFuenteYEditar(archivoFuente, archivoReal, nuevoID, otrosDatos);

            File carpetaOutput = new File("src/main/resources/output");
            if (!carpetaOutput.exists()) carpetaOutput.mkdirs();

            File copia = new File(carpetaOutput, archivoReal.getName());
            Files.copy(archivoReal.toPath(), copia.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            redirectAttributes.addFlashAttribute("mensaje", "Editado correctamente: " + rutaDestino);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error: " + e.getMessage());
        }

        return "redirect:/formulario4";
    }

    @GetMapping("/formulario4")
    public String formulario4(Model model) {
        return "editxml";
    }
}