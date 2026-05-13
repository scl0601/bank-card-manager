const { spawn } = require('child_process');
const cli = 'C:\\Users\\Administrator\\AppData\\Roaming\\npm\\node_modules\\@cloudbase\\cli\\bin\\tcb';
const data = JSON.stringify({
  domain: 'bankaiscl.top',
  routes: [{
    path: '/api',
    upstreamResourceType: 'CBR',
    upstreamResourceName: 'bank-admin-backend',
    enable: true,
    enableAuth: false,
    enableSafeDomain: true,
    enablePathTransmission: true
  }]
});
const child = spawn(process.execPath, [cli, '-e', 'dev-4g1sv3870175b971', 'routes', 'add', '--data', data], {
  stdio: 'inherit'
});
child.on('exit', code => process.exit(code ?? 1));
