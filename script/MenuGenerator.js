var fs = require('fs'); 
 
fs.readFile('api-docs.json', function(err, data) { 
  if (!err) { 
    var a = data.toString(); 
 
    var json = JSON.parse(a); 
 
    var newStr = ''; 
    for (var key in json.paths) { 
      var name = json.paths[`${key}`].get.summary; 
      newStr = newStr + 'name="' + name + '"\n'; 
    } 
 
    fs.writeFile('menu.txt', newStr, function(err, data) { 
      if (err) { 
        cosole.log(err); 
      } 
    }); 
  } 
}); 