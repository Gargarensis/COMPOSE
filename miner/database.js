const fs = require('fs');
const {
	join
} = require('path');
const mysqlSync = require('sync-mysql');
const readline = require('readline');

let con = new mysqlSync({
	host: 'localhost',
	user: 'root',
	password: ‘password’,
	port: 8889
});
let id = con.query(`SELECT MAX(id) as id FROM git_code.data;`)[0].id || 0;

const isDirectory = source => fs.lstatSync(source).isDirectory();
const getDirectories = source =>
	fs.readdirSync(source).map(name => join(source, name)).filter(isDirectory);

let results = getDirectories('results');

for (let i = 0; i < results.length; i++) {
	let subdirs = getDirectories(results[i]);

	console.log(`\nProcessing repository "${results[i]}": ${i + 1}/${results.length}`);
	for (let j = 0; j < subdirs.length; j++) {
		readline.clearLine(process.stdout, 0);
		readline.cursorTo(process.stdout, 0, null);
		process.stdout.write(`Processing commit: ${j + 1}/${subdirs.length}`);
		let data;
		try {
			data = fs.readFileSync(subdirs[j] + '/data.txt', 'utf8');
		} catch (err) {
			console.log('There was an error while reading the diff file:');
			console.log(err);
			continue;
		}
		data = JSON.parse(data);

		let file = 1;
		let running = true;
		while (running) {
			let code = "";
			let comments = "";
			if (fs.existsSync(subdirs[j] + '/code' + file + '.txt')) {
				let fileRead = fs.readFileSync(subdirs[j] + '/code' + file + '.txt', 'utf8');
				fileRead = fileRead.replace(/^>/gm, "");
				code = mysql_real_escape_string(fileRead);
			}
			if (fs.existsSync(subdirs[j] + '/comments' + file + '.txt')) {
				let fileRead = fs.readFileSync(subdirs[j] + '/comments' + file + '.txt', 'utf8');
				fileRead = fileRead.replace(/^>/gm, "");
				comments = mysql_real_escape_string(fileRead);
			}

			comments = comments.replace("\r?\n", " ");

			if (code == "" && comments == "") {
				running = false;
			} else {
				let queryResult = con.query(`INSERT INTO data (id, message, code, comments, hash, url, name) VALUES (${++id}, "${mysql_real_escape_string(data.message)}", "${code}", "${comments}", "${data.hash}", "${data.url}", "${mysql_real_escape_string(data.name)}");`);
				//console.log(queryResult);
				// let dataLine = mysql_real_escape_string(data.message).trim() + " " + comments.trim();
				// dataLine = basicTextNormalization(dataLine);
				// dataLine = removeStopwordList(dataLine, "./git-code-servlet/data/stop-words-english.txt");

				// let csvLine = id + ",\"" + dataLine + "\"\n";

				// stream.write(csvLine);
			}
			file++;
		}
	}
	console.log('\nDone!\n')
}

console.log("Building the corpus file for the servlet...");

// create file if it does not exist, empty it if it does
fs.closeSync(fs.openSync('../servlet/data/corpus.csv', 'w'));

let stream = fs.createWriteStream("../servlet/data/corpus.csv", {
	flags: 'a'
});

let sR = con.query(`SELECT id, message, comments FROM data LIMIT 0, 18446744073709551610;`);

for (let i = 0; i < sR.length; i++) {
	let message = sR[i].message || "";
	let comments = sR[i].comments || "";
	let dataLine = mysql_real_escape_string(message).trim() + " " + comments.trim();
	dataLine = basicTextNormalization(dataLine);
	dataLine = removeStopwordList(dataLine, "../servlet/data/stop-words-english.txt");

	let csvLine = sR[i].id + ",\"" + dataLine + "\"\n";

	stream.write(csvLine);
	readline.clearLine(process.stdout, 0);
	readline.cursorTo(process.stdout, 0, null);
	process.stdout.write(`Processing row: ${i + 1}/${sR.length}`);
}

stream.end();
con.dispose();
console.log('\nDone!\n')

function basicTextNormalization(toClean) {

	//Remove punctuation and numbers
	let cleanedString = toClean.replace(/[^a-zA-Z ]/gm, "");
	//Replace all multiple spaces with one space
	cleanedString = cleanedString.replace(/\s+/gm, " ");
	//Replace new lines with one space
	cleanedString = cleanedString.replace(/[\t\n\r]+/gm, " ");
	//Put all to lowercase
	cleanedString = cleanedString.toLowerCase();
	//Remove comments stuff
	cleanedString = cleanedString.replace(/[\/\*]/g, " ");
	//remove single letters
	cleanedString = " " + cleanedString + " ";
	cleanedString = cleanedString.replace(/\s[a-z]\s/gm, " ");

	return cleanedString;
}


function removeStopwordList(toClean, stopWordListPath) {

	let cleanedString = toClean.trim().toLowerCase();
	let stopwords = fs.readFileSync(stopWordListPath, "utf8");
	let lines = stopwords.split('\n');

	for (let i = 0; i < lines.length; i++) {
		cleanedString = cleanedString.replace(new RegExp(" " + lines[i] + " ", "gm"), " ");
	}
	cleanedString = cleanedString.replace(/\s+/gm, " ");

	return cleanedString;
}

function mysql_real_escape_string(str) {
	if (typeof str != 'string')
		return str;

	return str.replace(/[\0\x08\x09\x1a\n\r"'\\\%]/g, function(char) {
		switch (char) {
			case "\0":
				return "\\0";
			case "\x08":
				return "\\b";
			case "\x09":
				return "\\t";
			case "\x1a":
				return "\\z";
			case "\n":
				return "\\n";
			case "\r":
				return "\\r";
			case "\"":
			case "'":
			case "\\":
			case "%":
				return "\\" + char; // prepends a backslash to backslash, percent,
				// and double/single quotes
		}
	});
}
