# Visão Geral

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
![UML Diagram](https://github.com/mauricio-porto/OBDProxy/pictures/OBDProxy-UML.png "UML Diagram")


## Enumeração OBDCommand

O componente central é uma enumeração que reúne todos os comandos OBD. Esses comendos são utilizados para a inicialização do scanner OBD e para efetuar a leitura dos dados de diagnóstico que o scanner coleta.

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
