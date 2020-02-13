# POC : Lightweight Relationship Management
A POC to explore how we could manage relationships in lightweight way for some specifics usages.

This project is also a good way to understand how specific problem can be solved using A4C plugins and modifiers.

In some circonstances, we want to be able to weave relationship between some components, 
but without dealing with relationship operations, writes/read in temporary files to manage relationship 
target capability properties, or even write a specific type when we only need to feed a configuration file regarding relationships.

In such case, when all the information is known before deployment, we don't really need to implement relationship operations, 
and we don't want to have such useless steps in our workflows.

Our use case is :
- we have some component that can connect to a4c services.
- these components need a configuration file with variables. Theses variables must be
replaced by services capability property values.
- we want to use a generic component (sandbox mode), or specify this generic component in a proper type in order to override requirements cardinality.

How it works : 

- we have a base type for services (known as datastores)
- we have a base type for client, that has a configuration file (artifact).
- we define a base relationship type that describes such type of link. This relationship type has
a var named `var_mapping` of type map of string. This var_mapping stores the mapping between a target capability property and some vars in a config file (CSV).
- when we link a component to a service using such kind of relationship, we can graphically feed this mapping (using relationship property form).
- a modifier is in charge of exploring these relationship, resolve the corresponding target property value, and feed a map named `var_values`.
- this `var_values` contains a map (key is the var name, value is the value). It is used by a generic `configure` operation that do the replacement in the client configuration file.
- the `start` operation get the config file using an attribute and do it's job.

The plugin has all types in its [CSAR](src/main/resources/csar) to make the stuff work.

You can find a sample topology [Here](src/test/resources/csar/types.yml)

In this example, we have:
- 2 services that are our 2 specifications of datastore
- a node StandardClient that connects to the 2 datastores
- a node SpecificDbClient. It's type inherit from standard client type to redefine the requirements : this type need 1 and only 1 relationship to a service of type MyDb.

![Sample Topology](src/test/resources/doc/sampleTopolopgy.png?raw=true "Sample Topology")

If you look at the config file of the standard client, you can see that we use a [Jinja2](https://jinja.palletsprojects.com/en/2.11.x/) syntax. Actually we use jinja to replace variables in our config files.

```
db_username: {{ _.USERNAME }}
db_password: {{ _.PASSWORD }}
db_url: {{ _.MY_DB_IP }}:{{ _.MY_DB_PORT }}/{{ _.MY_DB_NAME }}
# This is another var containing db name : {{ _.MY_DATABASE_NAME }}
hdfs_url: {{ _.HDFS_IP }}/{{ _.HDFS_PATH }}
```

