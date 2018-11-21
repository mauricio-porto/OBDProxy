### Sumário

1. [Visão Geral](#overview)
2. [OBD-II PIDs](#obd-ii-pids)
3. [Diagrama UML](#diagrama-uml)
4. [Enumeração OBDCommand](#obdcommand)
5. [Como Usar](#como-usar)

# Overview

O OBDProxy é parte de um aplicativo de diagnóstico via OBD que fica consultando uma lista (ver *commandList*) de parâmetros de diagnóstico (ver OBD-II PIDs abaixo), como temperatura do motor, pressão do óleo, etc. e mostrando-os (ou enviando-os remotamente) a intervalos pequenos (tipicamente a cada 2 segundos).

Na versão implementada, a lista de parâmetros é fixa, sendo constituída por 7 tipos de informação sobre o funcionamento do motor, mas essa lista poderia ser modificada a qualquer tempo durante a execução do aplicativo.

Nós temos uma classe *OBDConnector* que fica enviando tal lista de consultas ao scanner OBD via Bluetooth e recebendo e tratando as respostas.

# OBD-II PIDs

[OBD-II PIDs](http://en.wikipedia.org/wiki/OBD-II_PIDs) são os códigos utilizados para consultar os dados de um veículo, usados em ferramentas de diagnóstico.

Esses códigos são classificados em tipos de serviço, conforme o link Wikipedia apresenta. Aqui vamos tratar apenas do serviço 01 - Mostrar dados atuais.

O programa cliente envia uma requisição de leitura passando o identificador do serviço e o PID (identificador do parâmetro) que deseja.

O scanner OBD então responde com um valor codificado que pode ter um número de bytes variável.

A resposta de cada parâmetro (PID) é codificada de forma a otimizar a velocidade e o uso de memória, muitas vezes exigindo uma fórmula para ser traduzida em valores com significado válido.
Por exemplo, o PID para a temperatura do ar na admissão (PID *0F*) retorna apenas um byte e precisa ter seu valor subtraído de 40, que irá indicar a temperatura em graus Celsius.

Disso resulta que precisamos ter um código específico para tratar e traduzir cada um dos parâmetros (PID) que desejarmos usar.

Minha solução foi concentrar o comportamento comum numa classe abstrata chamada *OBDResponseReader* e estender essa classe para cada PID desejado, implementando os métodos que traduzem o resultado lido para os valores e unidades necessários.

As classes especializadas em efetuar a tradução de cada parâmetro devem implementar 2 métodos, chamados *readResult* e *readFormattedResult* que retornam os valores brutos lidos (em bytes) para os valores convertidos com suas respectivas unidades. Por exemplo, RPM para a rotação do motor e ºC para temperaturas.

Esses 2 métodos de tradução dos resultados são declarados na interface *IResultReader* que as classes especializadas devem implementar.

Veja o exemplo para a classe *TemperatureReader*:
```
/**
 * @author mauricio
 *
 */
public class TemperatureReader extends OBDResponseReader implements IResultReader {

    private float temperature = 0.0f;

    @Override
    public String readResult(byte[] input) {
        String res = new String(input);
        return res;
    }

    @Override
    public String readFormattedResult(byte[] input) {
        String res = new String(input);

        if (!"NODATA".equals(res)) {
            temperature = getValue(input) - 40;  // It ranges from -40 to 215 °C
            
            res = String.format("%.0f", temperature);
        }

        return res;
    }

}
```

# Diagrama UML

O diagrama de classes abaixo mostra os principais componentes do modelo projetado para lidar com os objetos OBD no aplicativo OBDProxy.
![UML Diagram](https://github.com/mauricio-porto/OBDProxy/blob/master/pictures/OBDProxy-UML.png "UML Diagram")


## OBDCommand

O componente central é uma enumeração que reúne todos os comandos OBD. Esses comandos são utilizados para a inicialização do scanner OBD e para efetuar a leitura dos dados de diagnóstico que o scanner coleta.

A vantagem de usar uma enumeração é principalmente a eficiência, pois cada novo elemento de uma enumeração é bem mais leve do que uma nova classe.

O construtor dessa enumeração recebe 4 argumentos:

  - O nome do comando;
  - O código OBD do comando;
  - Uma referência a um tradutor de resultado, ou seja, uma classe que implementa a interface *IResultReader* adequadamente;
  - Um mnemônico para ser usado como identificador.
  
Veja um exemplo:
```
  AMBIENT_AIR_TEMPERATURE("Ambient Air Temperature", "01 46", new TemperatureReader(), "ambTemp")
```

Para acrescentar um novo parâmetro OBD a ser lido, tudo o que o programador precisa fazer é implementar um leitor de resultado para esse parâmetro e declarar um novo construtor como o exemplo acima.

Como exercício, vamos acrescentar o parâmetro posição do acelerador (Throttle position) ao modelo.

Inicialmente, precisamos criar a classe *ThrottlePositionReader* que estende a *OBDResponseReader* e implementa *IResultReader*.
Usando sua IDE preferida, a classe gerada automaticamente deverá se assemelhar a isto:
```
package com.braintech.obdproxy.base;

import com.braintech.obdproxy.IResultReader;

/**
 * @author mauricio
 *
 */
public class ThrottlePositionReader extends OBDResponseReader implements IResultReader {

    private int position;

    @Override
    public String readResult(byte[] input) {
       //TODO method stub
       return null;
    }

    @Override
    public String readFormattedResult(byte[] input) {
      //TODO method stub
      return null;
    }
}
```
Vamos preencher os métodos e fazer a conversão do valor retornado.

Segundo a página Wikipedia sobre os [OBD-II PIDs](https://en.wikipedia.org/wiki/OBD-II_PIDs), a posição do acelerador é dada em percentual e a conversão do byte lido A é feita por A * 100 / 255.

Assim os métodos preenchidos ficarão:
```
    @Override
    public String readResult(byte[] input) {
        String res = new String(input);
        return res;
    }

    @Override
    public String readFormattedResult(byte[] input) {
        String res = new String(input);

        if (!"NODATA".equals(res)) {
            position = (int) getValue(input) * 100 / 255;

            res = String.format("%d", position);
        }
        return res;
    }
```

Por último, precisamos acrescentar o construtor desse novo parâmetro na enumeração *OBDCommand*, desta forma:
```
THROTTLE_POSITION("Throttle posititon", "01 11", new ThrottlePositionReader(), "throtPos")
```

E voilà, está pronto, basta usar.

Fácil estender o modelo com novos parâmetros OBD, não?

# Como usar

Como dito anteriormente, a classe que se comunica com o scanner OBD é a *OBDConnector*.

Para enviar a lista de comandos OBD ao scanner e repassar as respostas ao serviço de notificação, eu criei uma instância de *Runnable* como segue:

```
private Runnable mQueueCommands = new Runnable() {
        public void run() {
            StringBuilder sb = new StringBuilder();
            sb.append('"').append("data").append('"').append(':').append('{');
            for (int i = 0; i < commandList.length; i++) {
                OBDCommand command = commandList[i];
                sb.append('"').append(command.getMnemo()).append('"').append(':').append('[');
                sb.append('"').append(command.getName()).append('"').append(',');
                String readParam = getOBDData(command);
                if (readParam != null) {
                    sb.append(readParam).append(']');
                } else {
                    sb.append(0).append(']');
                    Log.e(TAG, "\t\t " + command.getName() + " got null!!!");
                }
                if (i < commandList.length - 1) {
                    sb.append(',');
                }
            }
            sb.append('}');
            service.notifyDataReceived(sb.toString());
            // run again in 2s
            mHandler.postDelayed(mQueueCommands, 2000);
        }
    };
```

Minha lista de *OBDCommand* chamada *commandList*:
```
OBDCommand[] commandList = {
            OBDCommand.ENGINE_RPM,
            OBDCommand.ENGINE_LOAD,
            OBDCommand.SPEED,
            OBDCommand.AMBIENT_AIR_TEMPERATURE,
            OBDCommand.COOLANT_TEMPERATURE,
            OBDCommand.INTAKE_AIR_TEMPERATURE,
            OBDCommand.MAF};

```


Veja que por razões de performance, usei um _array_ para os *OBDCommand* que utilizei nesse protótipo, o que torna a lista fixa. Mas como dito no início, poderia usar uma lista dinâmica, como *ArrayList* por exemplo.

Voltando a examinar o *Runnable* _mQueueCommands_, notamos o uso do método *getOBDData(command)* para enviar um comando de leitura ao scanner OBD e retornar a resposta.

Eis sua implementação:

```
private String getOBDData(OBDCommand param) {
        if (param == null) {
            return null;
        }

        byte[] data = sendToDevice(param.getOBDcode());
        if (data != null && data.length > 0) {
            IResultReader reader = param.getReader();
            if (reader != null) {
                return reader.readFormattedResult(data);
            }
        }
        return null;
    }
```

Neste método, nós enviamos (_sendToDevice_) ao scanner por Bluetooth o código de leitura (_OBDCode_) associado ao parâmetro a ser lido e recebemos a resposta num _byte array_ que é verificado quanto a não ser nula a resposta. Caso não seja nula, tal resposta é encaminhada ao método _readFormattedResult(data)_ que irá então traduzir a resposta de acordo com o formato esperado, conforme descrito na seção [OBD-II PIDS](#obd-ii-pids).

Finalmente, no laço de execução do *Runnable mQueueCommands* a resposta obtida e convertida é enviada ao serviço de notificação pelo _service.notifyDataReceived()_. e uma nova execução é agendada para daí a 2 segundos.

