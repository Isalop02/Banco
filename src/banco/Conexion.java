/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banco;
import java.sql.*;
import javax.swing.JOptionPane;
/**
 *
 * @author isalo
 */
public class Conexion {
    // Datos de acceso a la base de datos
    private static final String URL = "jdbc:mysql://72.167.84.254:3306/banco";
    private static final String USUARIO = "isabela";
    private static final String CLAVE = "#3Programacion3";

    // Abre la conexión
    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL, USUARIO, CLAVE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error de conexión: " + e.getMessage());
            return null;
        }
    }

    // Pone en ocupado (1). Devuelve false si no existe o ya estaba ocupado
    public static boolean bloquear(String tabla, int id) throws Exception {
        Connection con = conectar();
        PreparedStatement ps = con.prepareStatement(
                "UPDATE " + tabla + " SET estado = 1 WHERE id = ? AND estado = 0");
        ps.setInt(1, id);
        int filas = ps.executeUpdate();
        con.close();
        return filas > 0;
    }

    // Pone en disponible (0)
    public static void liberar(String tabla, int id) throws Exception {
        Connection con = conectar();
        PreparedStatement ps = con.prepareStatement(
                "UPDATE " + tabla + " SET estado = 0 WHERE id = ?");
        ps.setInt(1, id);
        ps.executeUpdate();
        con.close();
    }
}
