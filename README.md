# **Comandos Disponíveis**

## **1. Comandos de Gerenciamento de Usuário**

* **REGISTER <fullName> <login> <email> <password>**
  **Propósito**: Registrar um novo usuário no sistema.
  **Exemplo**: REGISTER JohnDoe john [john@example.com](mailto:john@example.com) pass123
  **Respostas possíveis**:

  * `OK Registered successfully`
  * `ERROR Login already exists`
  * `ERROR Invalid register format`

* **LOGIN <login> <password>**
  **Propósito**: Autenticar um usuário no sistema.
  **Exemplo**: LOGIN john pass123
  **Respostas possíveis**:

  * `OK Logged in successfully`
  * `ERROR Invalid credentials`
  * `ERROR Invalid login format`

* **LOGOUT**
  **Propósito**: Desconectar o usuário atual, alterando seu status para "offline".
  **Exemplo**: LOGOUT
  **Respostas possíveis**:

  * `OK Logged out`
  * `ERROR Not logged in`

* **STATUS \<new\_status>**
  **Propósito**: Atualizar o status do usuário (e.g., "online", "busy", "away").
  **Exemplo**: STATUS busy
  **Respostas possíveis**:

  * `OK Status updated to <new_status>`
  * `ERROR Not logged in`

---

## **2. Comandos de Listagem**

* **LIST\_USERS**
  **Propósito**: Listar todos os usuários online.
  **Exemplo**: LIST\_USERS
  **Respostas possíveis**:

  * `OK user1,user2,user3`
  * `OK` (se não houver usuários online)

* **LIST\_GROUPS**
  **Propósito**: Listar todos os grupos existentes.
  **Exemplo**: LIST\_GROUPS
  **Respostas possíveis**:

  * `OK group1,group2,group3`
  * `OK` (se não houver grupos)

---

## **3. Comandos de Mensagens Privadas**

* **MSG\_USER <recipientLogin> <messageContent>**
  **Propósito**: Iniciar uma solicitação de chat privado com outro usuário.
  **Exemplo**: MSG\_USER mary Hello, Mary!
  **Respostas possíveis**:

  * `OK Message sent`
  * `OK Message queued for offline user`
  * `ERROR Invalid message format`

* **ACCEPT\_CHAT <senderLogin>**
  **Propósito**: Aceitar uma solicitação de chat privado de outro usuário.
  **Exemplo**: ACCEPT\_CHAT john
  **Respostas possíveis**:

  * `OK Chat accepted with <senderLogin>`

* **DECLINE\_CHAT <senderLogin>**
  **Propósito**: Recusar uma solicitação de chat privado.
  **Exemplo**: DECLINE\_CHAT john
  **Respostas possíveis**:

  * `OK Chat declined with <senderLogin>`

* **PMSG <recipientLogin> <messageContent>**
  **Propósito**: Enviar uma mensagem privada a um usuário após a solicitação ser aceita.
  **Exemplo**: PMSG mary Hi, how are you?
  **Respostas possíveis**:

  * `OK Private message sent`
  * `OK Private message queued for offline user`
  * `ERROR Invalid private message format`

---

## **4. Comandos de Gerenciamento de Grupo**

* **CREATE\_GROUP <groupName>**
  **Propósito**: Criar um novo grupo, com o usuário atual como administrador/membro inicial.
  **Exemplo**: CREATE\_GROUP friends
  **Respostas possíveis**:

  * `OK Group <groupName> created`
  * `ERROR Group already exists`

* **ADD\_TO\_GROUP <groupName> <userLogin>**
  **Propósito**: Convidar um usuário registrado para um grupo.
  **Exemplo**: ADD\_TO\_GROUP friends mary
  **Respostas possíveis**:

  * `OK User invited to group`
  * `ERROR Failed to add user to group`

* **JOIN\_GROUP\_REQUEST <groupName>**
  **Propósito**: Solicitar entrada em um grupo existente, requer aprovação de todos os membros.
  **Exemplo**: JOIN\_GROUP\_REQUEST friends
  **Respostas possíveis**:

  * `OK Join request sent`
  * `ERROR Failed to send join request`

* **GROUP\_INVITE\_RESPONSE <groupName> \<yes/no>**
  **Propósito**: Responder a um convite para entrar em um grupo.
  **Exemplo**: GROUP\_INVITE\_RESPONSE friends yes
  **Respostas possíveis**:

  * `OK Group invite response processed`
  * `ERROR Failed to process group invite response`

* **GROUP\_JOIN\_VOTE <groupName> <requestingUserLogin> \<yes/no>**
  **Propósito**: Votar na solicitação de entrada de um usuário em um grupo.
  **Exemplo**: GROUP\_JOIN\_VOTE friends mary yes
  **Respostas possíveis**:

  * `OK Vote processed`
  * `ERROR Failed to process vote`

* **LEAVE\_GROUP <groupName>**
  **Propósito**: Sair de um grupo do qual o usuário é membro.
  **Exemplo**: LEAVE\_GROUP friends
  **Respostas possíveis**:

  * `OK Left group <groupName>`
  * `ERROR Failed to leave group`

---

## **5. Comandos de Mensagens de Grupo**

* **MSG\_GROUP <groupName> <messageContent>**
  **Propósito**: Enviar uma mensagem para todos os membros online de um grupo.
  **Exemplo**: MSG\_GROUP friends Hello, group!
  **Respostas possíveis**:

  * `OK Group message sent`
  * `ERROR Invalid group message format`

* **MSG\_GROUP\_TARGETED <groupName> \<user1,user2> <messageContent>**
  **Propósito**: Enviar uma mensagem para usuários específicos dentro de um grupo.
  **Exemplo**: MSG\_GROUP\_TARGETED friends mary,alice Hi, selected friends!
  **Respostas possíveis**:

  * `OK Targeted group message sent`
  * `ERROR Invalid targeted group message format`

* **MSG\_GROUP\_PRIVATE <groupName>@<userLogin> <messageContent>**
  **Propósito**: Enviar uma mensagem privada a um membro do grupo, no contexto do grupo.
  **Exemplo**: MSG\_GROUP\_PRIVATE friends\@mary Private message in group
  **Respostas possíveis**:

  * `OK Private group message sent`
  * `ERROR Invalid private group message format`

---

## **6. Comando de Saída**

* **exit**
  **Propósito**: Encerrar a sessão do cliente, fechando a conexão com o servidor.
  **Exemplo**: exit
  **Resposta**: Nenhuma resposta do servidor; o cliente simplesmente fecha.

---

# **Respostas do Servidor**

* **NEW\_MSG <sender> <message>**
  Notifica o recebimento de uma mensagem privada.
  **Exemplo**: NEW\_MSG john Hello, Mary!

* **NEW\_GROUP\_MSG <groupName> <sender> <timestamp> <message>**
  Notifica o recebimento de uma mensagem de grupo.
  **Exemplo**: NEW\_GROUP\_MSG friends john 1623456789 Hello, group!

* **USER\_STATUS\_UPDATE <userLogin> <newStatus>**
  Notifica mudanças de status de outros usuários.
  **Exemplo**: USER\_STATUS\_UPDATE john busy

* **GROUP\_INVITE <groupName> <inviterLogin>**
  Notifica um convite para entrar em um grupo.
  **Exemplo**: GROUP\_INVITE friends john

* **USER\_LEFT\_GROUP <groupName> <userLogin>**
  Notifica que um usuário saiu de um grupo.
  **Exemplo**: USER\_LEFT\_GROUP friends mary

* **ERROR <message>**
  Indica um erro genérico.
  **Exemplo**: ERROR Unknown command

---

Esse formato foi projetado para facilitar a leitura e compreensão dos comandos e suas funções no sistema.
