Feature: Gestão de pedidos de recolha de lixo

  Scenario: Criar, consultar e cancelar um pedido válido
    Given que estou na página inicial do cidadão
    When preencho o formulário com "Lisboa", "Teste funcional", "2025-11-19", "09:00-11:00"
    And submeto o pedido
    Then devo ver uma mensagem de sucesso com um código
    When consulto o pedido com o token retornado
    Then devo ver o pedido com descrição "Teste funcional" e horário "09:00-11:00"
    When cancelo o pedido
    Then o estado do pedido deve ser "CANCELADO"

  Scenario: Rejeitar pedido com município inválido
    Given que estou na página inicial do cidadão
    When preencho o formulário com "Atlantis", "Pedido teste inválido", "2025-11-20", "09:00-11:00"
    And submeto o pedido
    Then devo ver a mensagem de erro "Município é obrigatório"

  Scenario: Rejeitar pedido com descrição demasiado curta
    Given que estou na página inicial do cidadão
    When preencho o formulário com "Lisboa", "Oi", "2025-11-21", "10:00-11:00"
    And submeto o pedido
    Then devo ver a mensagem de erro "Descrição inválida"

  Scenario: Consultar pedido inexistente
    Given que estou na página inicial do cidadão
    When consulto o pedido com token "TOKEN_INEXISTENTE_123"
    Then devo ver a mensagem "Reserva não encontrada"
