import java.nio.file.*;
import net.sf.jasperreports.engine.JasperCompileManager;
/** Executar com o classpath Maven; mantém os .jasper distribuídos sincronizados. */
public class CompilarRelatorios {
 public static void main(String[] args) throws Exception {
  Path raiz=Paths.get("src/main/resources/jxml");
  try(java.util.stream.Stream<Path> paths=Files.walk(raiz)) {
   for(Path p:(Iterable<Path>)paths.filter(f->f.toString().endsWith(".jrxml"))::iterator) {
    Path destino=Paths.get("src/main/resources/jasper").resolve(raiz.relativize(p).toString().replace(".jrxml",".jasper"));
    Files.createDirectories(destino.getParent());JasperCompileManager.compileReportToFile(p.toString(),destino.toString());System.out.println(destino);
   }
  }
 }
}
