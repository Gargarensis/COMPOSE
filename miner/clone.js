const fs = require('fs');
const cmd = require('node-cmd');
const exec = require('child_process').exec;
const sleep = require('sleep');
const readline = require('readline');
// const commentPattern = new RegExp(
// 	'(\\/\\*([^*]|[\\r\\n]|(\\*+([^*/]|[\\r\\n])))*\\*+/)|(//.*)',
// 	'g'
// );
const commentPattern = RegExp('/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/|//.*', 'g');
const Promise = require('bluebird');
const rimraf = require('rimraf');
const execSync = require('child_process').execSync;

const getAsync = Promise.promisify(cmd.get, {
	multiArgs: true,
	context: cmd
});

function systemSync(cmd) {
	try {
		return execSync(cmd, {
			stdio: ['pipe', 'pipe', 'ignore']
		}).toString();
	} catch (error) {
		console.log('There was an error while executing:\n' + cmd + '\n');
		console.log('Status: ' + error.status); 
		console.log('Message: ' + error.message); 
		console.log('Stderr: ' + error.stderr); 
		console.log('Stdout: ' + error.stdout); 
	}
	return undefined;
};

let javaRepos = JSON.parse(fs.readFileSync('./gits.json', 'utf8')).gits;

cloneAllRepos();

function cloneAllRepos() {
	if (fs.existsSync('./stats.csv')) {
		fs.unlinkSync('./stats.csv'); //remove stat file so we can rewrite it
	}

	function next() {
		if (javaRepos.length == 0) {
			return;
		}
		cloneRepo(javaRepos.pop(), next);
	}

	cloneRepo(javaRepos.pop(), next);
}

function elaborateCommits(name, data, url, callback) {
	let commits = JSON.parse(data);

	for (let i = commits.length - 1; i >= 0; i--) {

		readline.clearLine(process.stdout, 0);
		readline.cursorTo(process.stdout, 0, null);
		process.stdout.write(`Processing commits of ${name}: ${commits.length - i}/${commits.length}`);


		if (commits[i].parent != '') {

			let diff = systemSync(`
            cd cloned/${name}
            git diff ${commits[i].parent} ${commits[i].commit} --stat --stat-width=2000
            `);

			if (diff) {
				if (diff.indexOf('1 file changed') >= 0) {

					let commitData = diff.split('\n');
					let filePath = commitData[0].split('|')[0].trim();

					let fileName = filePath.split('\\').pop().split('/').pop();


					if (fileName.split('.').pop() != 'java') {
						continue;
					}

					if (!fs.existsSync('./results/' + name)) {
						fs.mkdirSync('./results/' + name);
					}

					if (!fs.existsSync('./results/' + name + '/' + commits[i].commit)) {
						fs.mkdirSync('./results/' + name + '/' + commits[i].commit);
						fs.writeFile('./results/' + name + '/' + commits[i].commit + '/data.txt', JSON.stringify({
							message: commits[i].message,
							name: name,
							hash: commits[i].commit,
							url: url
						}), 'utf8', function(err) {
							if (err) {
								console.log('There was an error while writing the commit message: ');
								console.log(err);
							}
						});
					}

					let baDiff = systemSync(`
							cd cloned/${name}
							git show ${commits[i].parent}:"${filePath}" > "../../results/${name}/${commits[i].commit}/before_${fileName}"
							git show ${commits[i].commit}:"${filePath}" > "../../results/${name}/${commits[i].commit}/after_${fileName}"
							diff "../../results/${name}/${commits[i].commit}/before_${fileName}" "../../results/${name}/${commits[i].commit}/after_${fileName}" > "../../results/${name}/${commits[i].commit}/diff.txt"
							echo "Done"
						`);

					if (baDiff) {
						analyzeDiff(`./results/${name}/${commits[i].commit}`, commits[i].message);
					}
				}
			}
		}
	}


	console.log('\nDeleting repo folder..');
	rimraf.sync(`./cloned/${name}`);
	console.log('\nDone!\n');

	if (callback)
		callback();
	return;
}

function analyzeDiff(directory, message) {

	let data;
	try {
		data = fs.readFileSync(directory + '/diff.txt', 'utf8');
	} catch (err) {
		console.log('There was an error while reading the diff file:');
		console.log(err);
		return;
	}

	if (!data || data == null)
		return;

	let datas = data.split('---');

	let fileCount = 0;

	for (let i = 0; i < datas.length; i++) {
		fileCount++;
		let code = datas[i].match(/^>.*/gm);
		if (!code || code == null) {
			fileCount--;
			continue;
		}

		let linesOfCode = code.length;

		code = code.join('\n');

		if (!code.toLowerCase().match(/[a-z]/i)) {
			continue;
		}

		try {
			fs.writeFileSync(directory + '/code' + fileCount + '.txt', code, 'utf8');
		} catch (err) {
			if (err) {
				console.log('There was an error while writing the code file: ' + i);
				console.log(err);
			}
		}

		if (!code || code == null || code == '')
			continue;

		let comments = code.match(commentPattern);

		if (!comments || comments == null) {
			continue;
		}
		comments = comments.join('\n');

		try {
			fs.writeFileSync(directory + '/comments' + fileCount + '.txt', comments, 'utf8');
		} catch (err) {
			if (err) {
				console.log('There was an error while writing the comment file: ' + i);
				console.log(err);
			}
		}

		// comment this to generate stat table
		continue;

		if (linesOfCode > 15 || linesOfCode < 3)
			continue;

		if (linesOfCode == 1 && code.trim().indexOf('//') >= 0)
			continue;

		let excludedKeywords = fs.readFileSync('./excluded_keywords.txt', 'utf8');
		excludedKeywords = excludedKeywords.split('\n');

		let containsExcluded = false;

		for (let i = 0; i < excludedKeywords.length && !containsExcluded; i++) {
			if (comments.indexOf(excludedKeywords[i]) >= 0) {
				containsExcluded = true;
			}
		}

		if (containsExcluded)
			continue;

		let toleranceKeywords = fs.readFileSync('./tolerance_keywords.txt', 'utf8');
		toleranceKeywords = toleranceKeywords.split('\n');

		let javaAppearing = 0;

		for (let i = 0; i < toleranceKeywords.length; i++) {
			if (comments.indexOf(toleranceKeywords[i]) >= 0) {
				javaAppearing++;
			}
		}

		if (javaAppearing > 5) {
			continue;
		}

		// filters like not more than 5 java keywords or more than 10 lines added or something like that
		// do it also on https://github.com/pcqpcq/open-source-android-apps
		if (Math.floor(Math.random() * (100 - 0 + 1) + 0) <= 3) {
			fs.appendFileSync('./stats.csv', `"${code.replace(/"/g, "'")}","${comments.replace(/"/g, "'")}","${message.replace(/"/g, "'")}"\n`, 'utf8');
		}
	}
}

function cloneRepo(repo, callback) {
	console.log('\n');
	console.log('============================================');
	console.log('\n');
	console.log('Cloning ' + repo.split('/').pop() + '...');
	getAsync(
		`
         cd cloned
         git clone ${repo}
         cd ${repo.split('/').pop().slice(0, -4)}
         git log --pretty=format:'{#@##@commit#@##@: #@##@%H#@##@,  #@##@abbreviated_commit#@##@: #@##@%h#@##@,  #@##@tree#@##@: #@##@%T#@##@,  #@##@abbreviated_tree#@##@: #@##@%t#@##@,  #@##@parent#@##@: #@##@%P#@##@,  #@##@abbreviated_parent#@##@: #@##@%p#@##@,  #@##@refs#@##@: #@##@%D#@##@,  #@##@encoding#@##@: #@##@%e#@##@,  #@##@message#@##@: #@##@%s#@##@,  #@##@sanitized_subject_line#@##@: #@##@%f#@##@,  #@##@body#@##@: #@##@%b#@##@,  #@##@commit_notes#@##@: #@##@%N#@##@,  #@##@verification_flag#@##@: #@##@%G?#@##@,  #@##@signer#@##@: #@##@%GS#@##@,  #@##@signer_key#@##@: #@##@%GK#@##@,  #@##@author#@##@: {    #@##@name#@##@: #@##@%aN#@##@,    #@##@email#@##@: #@##@%aE#@##@,    #@##@date#@##@: #@##@%aD#@##@  },  #@##@commiter#@##@: {    #@##@name#@##@: #@##@%cN#@##@,    #@##@email#@##@: #@##@%cE#@##@,    #@##@date#@##@: #@##@%cD#@##@  }},' > commits.json
      `).then(data => {

		console.log('Repo ', repo.split('/').pop() + ' cloned!\n');

		// link to repo is not valid anymore
		if (!fs.existsSync('./cloned/' + repo.split('/').pop().slice(0, -4))) {
			console.log('Cannot find repo folder. Probably it has been moved or removed from GitHub.');
			if (callback)
				callback();
			return;
		}

		console.log(repo);

		// commits.json not found
		if (!fs.existsSync('./cloned/' + repo.split('/').pop().slice(0, -4) + '/commits.json')) {
			console.log('Cannot find commits.json file.');
			if (callback) callback();
			return;
		}

		commits = '[' + fs.readFileSync('./cloned/' + repo.split('/').pop().slice(0, -4) + '/commits.json', 'utf8').slice(0, -1) + ']';
		fs.writeFileSync('./cloned/' + repo.split('/').pop().slice(0, -4) + '/commits.json', sanitizeJSON(commits));
		elaborateCommits(repo.split('/').pop().slice(0, -4), sanitizeJSON(commits), repo, callback);
	}).catch(err => {
		console.log('There was an error while cloning repo: ' + repo);
		console.log(err);
	});
}

function sanitizeJSON(unsanitized) {
	unsanitized = unsanitized.replace(/\\/g, '\\\\').replace(/\n/g, '').replace(/\r/g, '\\r').replace(/\t/g, '\\t').replace(/\f/g, '\\f').replace(/"/g, '\\"');
	unsanitized = unsanitized.replace(/'/g, '\x27').replace(/\&/g, '\x26');
	// remove non-printable and other non-valid JSON chars
	unsanitized = unsanitized.replace(/[\u0000-\u0019]+/g, '');
	unsanitized = unsanitized.replace(new RegExp('#@##@', 'g'), '"').replace(/#/g, '');
	return unsanitized;
}
