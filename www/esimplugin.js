module.exports = {
  hasEsimEnabled: function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'EsimPlugin', 'hasEsimEnabled', []);
  }
};