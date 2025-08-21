API responsável por receber e iniciar o processamento de solicitações de consulta de custódia de instrumentos financeiros. A chamada da API é síncrona, retornando imediatamente um identificador único para rastreamento da solicitação. O processamento da requisição e o envio das notificações são realizados de forma assíncrona nos sistemas internos, e após a conclusão, os dados da consulta são disponibilizados na plataforma E-Watcher para entrega ao cliente.

Utilizado para submeter uma nova solicitação de consulta de custódia, enviando os dados da conta e dos ativos no corpo da requisição (body) para iniciar o processamento.


/api-external/custody-query/v1/request/solicitations
