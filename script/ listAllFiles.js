var fs = require('fs')
var os_path = require('path')

function listAllFiles(path) {
	fs.readdir(path, (err, files) => {
		if (!err) {
			files.forEach(file => {
				fs.stat(file, (err, stats) => {
					console.log(file)
					const filePath = path + os_path.sep +file
					if (!err) {
						if(stats.isDirectory()) {
							console.log(filePath)
							listAllFiles(filePath)
						} else {
							fs.readFile(file, 'utf8', (err, data) => {
								if(!err) {
									console.log(data);
								}
							});
						}
					}
				})
			})
		}
	})
}

listAllFiles('.')