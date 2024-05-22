# Pedidos e Pagamentos

## Recursos e Bibliotecas
- [x] Java 17
- [x] Document DB
- [x] SQS
- [x] Spring Boot
- [x] MapStruct
- [x] Vavr
- [x] JsonPatch


## Dicionário de Linguagem Ubíqua

Termos utilizados na implementação (Presentes em Código)

- **Cliente/Customer**: O consumidor que realiza um pedido no restaurante.
- **Pedido/Order**: A lista de produtos (seja uma bebida, lanche, acompanhamento e/ou sobremesa) realizada pelo cliente no restaurante.
- **Produto/Product**: Item que será consumido pelo cliente, que se enquadra dentro de uma categoria, como por exemplo: bebida, lanche, acompanhamento e/ou sobremesa.
- **Categoria/Product Type**: Como os produtos são dispostos e gerenciados pelo estabelecimento: bebidas, lanches, acompanhamentos e/ou sobremesas.
- **Esteira de Pedidos/Order Tracking**: Responsável pelo andamento e monitoramento do estado do pedido.
- **Funcionário/Employee**: Funcionário do estabelecimento.

## Operações

### [Faturamento]([BillingController.java](fastfood-api%2Fsrc%2Fmain%2Fjava%2Fio%2Ffiap%2Ffastfood%2Fdriver%2Fcontroller%2Fbilling%2FBillingController.java))
Tratando-se apenas de um esboço, prevê a abertura e o fechamento de um dia contábil.

Um dia pode ser aberto ao se chamar a operação "open" e fechado chamando o método "close", ambos do serviço BillingService, este exposto via gRPC e descrito aqui [fastfood-billing.proto](fastfood-order-api%2Fsrc%2Fmain%2Fproto%2Ffastfood-billing.proto).


#### Contador de pedidos
Ao se abrir/fechar um dia contábil o contador de pedidos é zerado, gerando assim números mais amigáveis aos clientes.

### [Pedido e Pagamento]([OrderController.java](fastfood-api%2Fsrc%2Fmain%2Fjava%2Fio%2Ffiap%2Ffastfood%2Fdriver%2Fcontroller%2Forder%2FOrderController.java))
Uma criação de um novo pedido, é sempre acompanhada das informações necessárias ao encaminhamento de um pagamento. Por isso, a fim de se manter a persistência em uma única transação, um mesmo endpoint compreende a criação de ambas as entidades.
Uma vez persistidas as informações de pedido e pagamento, uma requisição é enviada ao parceiro de pagamentos.

As operações de pedido são expostas via gRPC e estão descritas no arquivo [fastfood-order.proto](fastfood-order-api%2Fsrc%2Fmain%2Fproto%2Ffastfood-order.proto).

#### Webhook de Pagamentos
Ao encaminhar o pagamento para processamento, em conjunto com as informações do pedido, é se encaminhado um endpoint com fim de receber a resposta do processo.
No recebimento de respostas do parceiro de pagamentos por meio do Webhook, atualiza-se o estado do pagamento na base e então se encaminha uma mensagem à api de rastreio.

## Início rápido

```shell 
docker-compose up
```
Os servições gRPC são expostos em [localhost:9090](http://localhost:9090).

## Deploy

O deploy pode ser realizado através da execução do pipeline "Deploy product" no Github Actions.
No entanto, anteriormente a execução, faz-se necessária a configuração do ID e SECRET da AWS nos secrets do repositório.
Como o acesso às variáveis e secrets do respositório é limitado ao owner e maintainers, recomendo a execução dos passos do script de deploy localmente com apontamento para a cloud.
Seguem abaixo os passos:

1 -
```
./mvnw clean install -Dmaven.test.skip=true -U -P dev
```
2 -
```
docker login registry-1.docker.io
```
3 -
```
aws eks update-kubeconfig --name {CLUSTER_NAME} --region={AWS_REGION}
```
4 -
```
helm upgrade --install fastfood-order charts/fastfood-order \
--kubeconfig /home/runner/.kube/config \
--set containers.image=icarodamiani/fastfood-bff \
--set image.tag=latest \
--set database.mongodb.username.value=fastfood \
--set database.mongodb.host.value={AWS_DOCUMENTDB_HOST} \
--set database.mongodb.password.value={AWS_DOCUMENTDB_PASSWORD}
```
