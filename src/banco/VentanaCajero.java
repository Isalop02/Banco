/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package banco;
import java.sql.*;
/**
 *
 * @author isalo
 */
public class VentanaCajero extends javax.swing.JFrame {
    String monto = "";
    boolean enRetiro = false;   // true cuando cliente y cajero están bloqueados
    int idCliente, idCajero;
    String nombreCliente;
    double saldoCliente;
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(VentanaCajero.class.getName());

    /**
     * Creates new form VentanaCajero
     */
    public VentanaCajero() {
        initComponents();
    }
    // Busca un cliente por número de cuenta y guarda sus datos
private boolean buscarCliente(String cuenta) throws Exception {
    Connection con = Conexion.conectar();
    PreparedStatement ps = con.prepareStatement("SELECT * FROM clientes WHERE ncuenta = ?");
    ps.setInt(1, Integer.parseInt(cuenta));
    ResultSet rs = ps.executeQuery();
    boolean existe = rs.next();
    if (existe) {
        idCliente = rs.getInt("id");
        nombreCliente = rs.getString("nombre") + " " + rs.getString("apellido");
        saldoCliente = rs.getDouble("saldo");
    }
    con.close();
    return existe;
}

// Libera cliente y cajero si estaban bloqueados
private void liberarTodo() {
    try {
        if (enRetiro) {
            Conexion.liberar("clientes", idCliente);
            Conexion.liberar("cajeros", idCajero);
        }
    } catch (Exception e) { }
    enRetiro = false;
    monto = "";
}
    private void hacerRetiro() throws Exception {
    if (monto.isEmpty()) {
        pantalla.setText("Cliente: " + nombreCliente + "\nDigite un monto\n\nMonto: ");
        return;
    }
    int valor = Integer.parseInt(monto);

    // Validar mínimo, máximo y múltiplo
    if (valor < 10000 || valor > 1000000 || valor % 10000 != 0) {
        monto = "";
        pantalla.setText("Error: el monto debe estar entre 10.000 y 1.000.000"
                + "\ny ser múltiplo de 10.000\n\nMonto: ");
        return;
    }
    // Validar saldo del cliente
    if (valor > saldoCliente) {
        monto = "";
        pantalla.setText("Error: saldo insuficiente. Saldo: $" + dinero(saldoCliente) + "\n\nMonto: ");
        return;
    }

    // Leer los billetes del cajero (orden: 100k, 50k, 20k, 10k)
    Connection con = Conexion.conectar();
    PreparedStatement ps = con.prepareStatement("SELECT * FROM cajeros WHERE id = ?");
    ps.setInt(1, idCajero);
    ResultSet rs = ps.executeQuery();
    rs.next();
    int[] disponibles = {rs.getInt("ncien"), rs.getInt("ncincuenta"), rs.getInt("nveinte"), rs.getInt("ndiez")};
    con.close();

    // Validar dinero del cajero
    int totalCajero = disponibles[0] * 100000 + disponibles[1] * 50000
                    + disponibles[2] * 20000 + disponibles[3] * 10000;
    if (valor > totalCajero) {
        monto = "";
        pantalla.setText("Error: el cajero no tiene suficiente dinero\n\nMonto: ");
        return;
    }

    int[] entregar = calcularBilletes(valor, disponibles);
    if (entregar == null) {
        monto = "";
        pantalla.setText("Error: el cajero no tiene los billetes para ese monto\n\nMonto: ");
        return;
    }

    // Actualizar saldo del cliente y billetes del cajero
    con = Conexion.conectar();
    ps = con.prepareStatement("UPDATE clientes SET saldo = saldo - ? WHERE id = ?");
    ps.setInt(1, valor);
    ps.setInt(2, idCliente);
    ps.executeUpdate();
    ps = con.prepareStatement(
        "UPDATE cajeros SET ncien = ncien - ?, ncincuenta = ncincuenta - ?, "
      + "nveinte = nveinte - ?, ndiez = ndiez - ? WHERE id = ?");
    ps.setInt(1, entregar[0]);
    ps.setInt(2, entregar[1]);
    ps.setInt(3, entregar[2]);
    ps.setInt(4, entregar[3]);
    ps.setInt(5, idCajero);
    ps.executeUpdate();
    con.close();

    pantalla.setText("Retiro exitoso\nCliente: " + nombreCliente
            + "\nValor: $" + dinero(valor)
            + "\n\nBilletes entregados:"
            + "\n  100.000 x " + entregar[0]
            + "\n   50.000 x " + entregar[1]
            + "\n   20.000 x " + entregar[2]
            + "\n   10.000 x " + entregar[3]
            + "\n\nSaldo restante: $" + dinero(saldoCliente - valor));

    liberarTodo(); // vuelve cliente y cajero a disponibles
}

// Calcula cuántos billetes de cada uno entregar. Devuelve null si no se puede
private int[] calcularBilletes(int valor, int[] disponibles) {
    int[] valores = {100000, 50000, 20000, 10000};
    int[] entregar = {0, 0, 0, 0};

    // Reglas especiales del enunciado
    if (valor == 50000) {
        entregar[2] = 2;   // dos de 20.000
        entregar[3] = 1;   // uno de 10.000
    } else if (valor >= 100000) {
        entregar[1] = 1;   // uno de 50.000
        entregar[2] = 2;   // dos de 20.000
        entregar[3] = 1;   // uno de 10.000
    }

    // Lo que falta por entregar
    int resto = valor;
    for (int i = 0; i < 4; i++) {
        resto = resto - entregar[i] * valores[i];
    }

    // Completar con los billetes más grandes posibles (menor cantidad de billetes)
    for (int i = 0; i < 4; i++) {
        int quedan = disponibles[i] - entregar[i];
        if (quedan < 0) quedan = 0;
        int cantidad = Math.min(resto / valores[i], quedan);
        entregar[i] = entregar[i] + cantidad;
        resto = resto - cantidad * valores[i];
    }

    // Verificar que se completó y que hay billetes suficientes
    if (resto != 0) return null;
    for (int i = 0; i < 4; i++) {
        if (entregar[i] > disponibles[i]) return null;
    }
    return entregar;
}
// Formato de dinero: 1500000 -> 1.500.000
private String dinero(double valor) {
    return String.format("%,.0f", valor);
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtCuenta = new javax.swing.JTextField();
        txtCajero = new javax.swing.JTextField();
        txtDestino = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        pantalla = new javax.swing.JTextArea();
        btn1 = new javax.swing.JButton();
        btn2 = new javax.swing.JButton();
        btn3 = new javax.swing.JButton();
        btn4 = new javax.swing.JButton();
        btn5 = new javax.swing.JButton();
        btn6 = new javax.swing.JButton();
        btn7 = new javax.swing.JButton();
        btn8 = new javax.swing.JButton();
        btn9 = new javax.swing.JButton();
        btnBorrar = new javax.swing.JButton();
        btn0 = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        btnSaldo = new javax.swing.JButton();
        btnConsignar = new javax.swing.JButton();
        btnRetirar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        btnTransferir = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("N° de cuenta:");

        jLabel2.setText("N° de cajero:");

        jLabel3.setText("Cuenta destino:");

        pantalla.setEditable(false);
        pantalla.setBackground(new java.awt.Color(51, 51, 51));
        pantalla.setColumns(20);
        pantalla.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        pantalla.setForeground(new java.awt.Color(0, 204, 0));
        pantalla.setRows(5);
        jScrollPane1.setViewportView(pantalla);

        btn1.setText("1");
        btn1.addActionListener(this::btn1ActionPerformed);

        btn2.setText("2");
        btn2.addActionListener(this::btn2ActionPerformed);

        btn3.setText("3");
        btn3.addActionListener(this::btn3ActionPerformed);

        btn4.setText("4");
        btn4.addActionListener(this::btn4ActionPerformed);

        btn5.setText("5");
        btn5.addActionListener(this::btn5ActionPerformed);

        btn6.setText("6");
        btn6.addActionListener(this::btn6ActionPerformed);

        btn7.setText("7");
        btn7.addActionListener(this::btn7ActionPerformed);

        btn8.setText("8");
        btn8.addActionListener(this::btn8ActionPerformed);

        btn9.setText("9");
        btn9.addActionListener(this::btn9ActionPerformed);

        btnBorrar.setText("<---");
        btnBorrar.addActionListener(this::btnBorrarActionPerformed);

        btn0.setText("0");
        btn0.addActionListener(this::btn0ActionPerformed);

        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(this::btnLimpiarActionPerformed);

        btnSaldo.setText("Consultar saldo");
        btnSaldo.addActionListener(this::btnSaldoActionPerformed);

        btnConsignar.setText("Consignar");
        btnConsignar.addActionListener(this::btnConsignarActionPerformed);

        btnRetirar.setText("Transferir");
        btnRetirar.addActionListener(this::btnRetirarActionPerformed);

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(this::btnCancelarActionPerformed);

        btnTransferir.setText("Retirar");
        btnTransferir.addActionListener(this::btnTransferirActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtDestino, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(txtCajero, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(29, 29, 29)
                                .addComponent(txtCuenta, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnRetirar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnTransferir)
                                .addGap(31, 31, 31)
                                .addComponent(btnCancelar))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnSaldo)
                                .addGap(18, 18, 18)
                                .addComponent(btnConsignar))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(btn1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(btn4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)
                                        .addComponent(btn7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(btn2, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(btn5, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(28, 28, 28)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(btn6, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(btn3, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(btn8, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE)
                                            .addComponent(btn0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGap(28, 28, 28)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(btn9, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(btnLimpiar)))))
                            .addComponent(btnBorrar, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(65, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtCuenta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtCajero, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtDestino, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(29, 29, 29)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(48, 48, 48)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn1)
                    .addComponent(btn2)
                    .addComponent(btn3))
                .addGap(28, 28, 28)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn4)
                    .addComponent(btn5)
                    .addComponent(btn6))
                .addGap(32, 32, 32)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn7)
                    .addComponent(btn8)
                    .addComponent(btn9))
                .addGap(33, 33, 33)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnBorrar)
                    .addComponent(btn0)
                    .addComponent(btnLimpiar))
                .addGap(62, 62, 62)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSaldo)
                    .addComponent(btnConsignar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 34, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRetirar)
                    .addComponent(btnCancelar)
                    .addComponent(btnTransferir))
                .addGap(31, 31, 31))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn8ActionPerformed
        // TODO add your handling code here:
        monto = monto + "8";
pantalla.setText("Monto: " + monto);
    }//GEN-LAST:event_btn8ActionPerformed

    private void btn1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn1ActionPerformed
        // TODO add your handling code here:
        monto = monto + "1";
pantalla.setText("Monto: " + monto);
    }//GEN-LAST:event_btn1ActionPerformed

    private void btn2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn2ActionPerformed
        // TODO add your handling code here:
        monto = monto + "2";
pantalla.setText("Monto: " + monto);
    }//GEN-LAST:event_btn2ActionPerformed

    private void btn3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn3ActionPerformed
        // TODO add your handling code here:
        monto = monto + "3";
pantalla.setText("Monto: " + monto);
    }//GEN-LAST:event_btn3ActionPerformed

    private void btn4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn4ActionPerformed
        // TODO add your handling code here:
        monto = monto + "4";
pantalla.setText("Monto: " + monto);
    }//GEN-LAST:event_btn4ActionPerformed

    private void btn5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn5ActionPerformed
        // TODO add your handling code here:
        monto = monto + "5";
pantalla.setText("Monto: " + monto);
    }//GEN-LAST:event_btn5ActionPerformed

    private void btn6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn6ActionPerformed
        // TODO add your handling code here:
        monto = monto + "6";
pantalla.setText("Monto: " + monto);
    }//GEN-LAST:event_btn6ActionPerformed

    private void btn7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn7ActionPerformed
        // TODO add your handling code here:
        monto = monto + "7";
pantalla.setText("Monto: " + monto);
    }//GEN-LAST:event_btn7ActionPerformed

    private void btn9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn9ActionPerformed
        // TODO add your handling code here:
        monto = monto + "9";
pantalla.setText("Monto: " + monto);
    }//GEN-LAST:event_btn9ActionPerformed

    private void btnBorrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBorrarActionPerformed
        // TODO add your handling code here:
        if (!monto.isEmpty()) monto = monto.substring(0, monto.length() - 1);
     pantalla.setText("Monto: " + monto);
    }//GEN-LAST:event_btnBorrarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        // TODO add your handling code here:
        monto = "";
     pantalla.setText("Monto: ");
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnSaldoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaldoActionPerformed
        // TODO add your handling code here:
        try {
    if (buscarCliente(txtCuenta.getText())) {
        pantalla.setText("Cliente: " + nombreCliente + "\nSaldo: $" + dinero(saldoCliente));
    } else {
        pantalla.setText("Error: la cuenta no existe");
    }
} catch (Exception e) {
    pantalla.setText("Error: " + e.getMessage());
}
    }//GEN-LAST:event_btnSaldoActionPerformed

    private void btnConsignarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConsignarActionPerformed
        // TODO add your handling code here:
        try {
    if (!buscarCliente(txtCuenta.getText())) {
        pantalla.setText("Error: la cuenta no existe");
        return;
    }
    if (monto.isEmpty() || Integer.parseInt(monto) <= 0) {
        pantalla.setText("Digite el monto con el teclado\n\nMonto: ");
        return;
    }
    int valor = Integer.parseInt(monto);
    if (!Conexion.bloquear("clientes", idCliente)) {
        pantalla.setText("El cliente está ocupado, intente luego");
        return;
    }
    Connection con = Conexion.conectar();
    PreparedStatement ps = con.prepareStatement("UPDATE clientes SET saldo = saldo + ? WHERE id = ?");
    ps.setInt(1, valor);
    ps.setInt(2, idCliente);
    ps.executeUpdate();
    con.close();
    Conexion.liberar("clientes", idCliente);

    monto = "";
    pantalla.setText("Consignación exitosa\nCliente: " + nombreCliente
            + "\nNuevo saldo: $" + dinero(saldoCliente + valor));
} catch (Exception e) {
    pantalla.setText("Error: " + e.getMessage());
}
    }//GEN-LAST:event_btnConsignarActionPerformed

    private void btnRetirarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRetirarActionPerformed
        // TODO add your handling code here:
        try {
    if (!buscarCliente(txtDestino.getText())) {
        pantalla.setText("Error: la cuenta destino no existe");
        return;
    }
    int idDestino = idCliente;
    String nombreDestino = nombreCliente;

    if (!buscarCliente(txtCuenta.getText())) {
        pantalla.setText("Error: la cuenta origen no existe");
        return;
    }
    if (idCliente == idDestino) {
        pantalla.setText("Error: no puede transferir a la misma cuenta");
        return;
    }
    if (monto.isEmpty() || Integer.parseInt(monto) <= 0) {
        pantalla.setText("Digite el monto con el teclado\n\nMonto: ");
        return;
    }
    int valor = Integer.parseInt(monto);
    if (valor > saldoCliente) {
        pantalla.setText("Error: saldo insuficiente\nSaldo: $" + dinero(saldoCliente));
        return;
    }
    if (!Conexion.bloquear("clientes", idCliente)) {
        pantalla.setText("La cuenta origen está ocupada");
        return;
    }
    if (!Conexion.bloquear("clientes", idDestino)) {
        Conexion.liberar("clientes", idCliente);
        pantalla.setText("La cuenta destino está ocupada");
        return;
    }

    Connection con = Conexion.conectar();
    PreparedStatement ps = con.prepareStatement("UPDATE clientes SET saldo = saldo - ? WHERE id = ?");
    ps.setInt(1, valor);
    ps.setInt(2, idCliente);
    ps.executeUpdate();
    ps = con.prepareStatement("UPDATE clientes SET saldo = saldo + ? WHERE id = ?");
    ps.setInt(1, valor);
    ps.setInt(2, idDestino);
    ps.executeUpdate();
    con.close();

    Conexion.liberar("clientes", idCliente);
    Conexion.liberar("clientes", idDestino);

    monto = "";
    pantalla.setText("Transferencia exitosa\nDe: " + nombreCliente + "\nPara: " + nombreDestino
            + "\nValor: $" + dinero(valor) + "\nSu saldo: $" + dinero(saldoCliente - valor));
} catch (Exception e) {
    pantalla.setText("Error: " + e.getMessage());
}
    }//GEN-LAST:event_btnRetirarActionPerformed

    private void btnTransferirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTransferirActionPerformed
        // TODO add your handling code here:
        try {
    if (!enRetiro) {
        // PASO 1: validar y bloquear
        if (!buscarCliente(txtCuenta.getText())) {
            pantalla.setText("Error: la cuenta no existe");
            return;
        }
        idCajero = Integer.parseInt(txtCajero.getText());
        if (!Conexion.bloquear("clientes", idCliente)) {
            pantalla.setText("El cliente está ocupado");
            return;
        }
        if (!Conexion.bloquear("cajeros", idCajero)) {
            Conexion.liberar("clientes", idCliente);
            pantalla.setText("El cajero no existe o está ocupado");
            return;
        }
        enRetiro = true;
        monto = "";
        pantalla.setText("Cliente: " + nombreCliente
                + "\nDigite el monto y presione Retirar\n\nMonto: ");
    } else {
        // PASO 2: hacer el retiro
        hacerRetiro();
    }
} catch (Exception e) {
    liberarTodo();
    pantalla.setText("Error: " + e.getMessage());
}
    }//GEN-LAST:event_btnTransferirActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        // TODO add your handling code here:
        liberarTodo();
pantalla.setText("Operación cancelada\n\nMonto: ");
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btn0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn0ActionPerformed
        // TODO add your handling code here:
        monto = monto + "0";
pantalla.setText("Monto: " + monto);
    }//GEN-LAST:event_btn0ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new VentanaCajero().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn0;
    private javax.swing.JButton btn1;
    private javax.swing.JButton btn2;
    private javax.swing.JButton btn3;
    private javax.swing.JButton btn4;
    private javax.swing.JButton btn5;
    private javax.swing.JButton btn6;
    private javax.swing.JButton btn7;
    private javax.swing.JButton btn8;
    private javax.swing.JButton btn9;
    private javax.swing.JButton btnBorrar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnConsignar;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnRetirar;
    private javax.swing.JButton btnSaldo;
    private javax.swing.JButton btnTransferir;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea pantalla;
    private javax.swing.JTextField txtCajero;
    private javax.swing.JTextField txtCuenta;
    private javax.swing.JTextField txtDestino;
    // End of variables declaration//GEN-END:variables
}
