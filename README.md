# keycloak-realm-generator
This takes a keycloak export and transforms it so that you can re-import it into a new realm on the same keycloak instance, efectively allowing you to have something close to a template realm 

# Building
In order to build this project, all you need is java 11 and a recent version of maven, you just need to invoke 
`mvn package`
and maven will crate a standalone jar that you can re-use.

# Running
You just need to invoke
`java -jar realm-generator-0.0.1-SNAPSHOT-jar-with-dependencies.jar -h`
and follow the instructions.

If you run it without arguments, it will output a realm based on the incluced template. However, it you want to base the output on a different templatete, the path to the file can be supplied via the `-t` option.

# Customizing
If you want to redistribute a jar that will generate a realm based on your own internal spec instead of the provided example, all you need to do is replace the json file in the resources directory and comment the warning issued when loading that file on the `readTemplate()` method.

# Known issues
- If you changed the default scopes of any of the default clients generated when you create an empty realm, then export it and attempt to use it as a template, the import will fail. Keycloak seems to first create a default realm then attempt to apply the import without taking into account that some list are mutually exclusive (stuff that appears in one cannot appear in the other). This seems to take us into a situation where a scope appears on both the default scopes and on the available scopes and that is indeed an error. This problem has been verified on keycloak 5.0, but not yet on 6.0.

I am sure there are plenty of other issues, however for the initial purpose, this project was more than enough
