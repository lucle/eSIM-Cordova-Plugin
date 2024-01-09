module.exports = {
  hasEsimEnabled: function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'EsimPlugin', 'hasEsimEnabled', []);
  },
  installEsim: function(address, matchingID, iccid, confirmationCode, successCallback, errorCallback) {
      cordova.exec(successCallback, errorCallback, 'EsimPlugin', 'installEsim', [address, matchingID, iccid, confirmationCode]);
  },
  installEsimNew: function(matchingID, successCallback, errorCallback) {
      cordova.exec(successCallback, errorCallback, 'EsimPlugin', 'installEsimNew', [matchingID]);
  }
};
