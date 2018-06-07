# COMPOSE

To setup the server:
- Run the queries to build the database inside `database`
- Populate the `gits.json` file with the repositories you want to analyze
- Build the server (change database's credentials)
- (Optional or in case of path errors) After building, you can change the path of the output directories inside the `.js` files if you build it somewhere else
- Change the credentials for GitHub and database's access inside the `.js` files
- `npm install`
- Run `node clone.js`
- Run `node commit.js`
- (Optional) Import additional data into the database
- Run `node database.js`
- Start the server

To setup the plugin:
- Build it using IntelliJ
- Add it to IntelliJ
- Restart the IDE

# JetBrains Plugin Repository
A request is pending to add the plugin to the list of the official IntelliJ's plugins.