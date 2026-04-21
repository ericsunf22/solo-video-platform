import { Settings, Folder, Database, Info } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

export default function Settings() {
  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <Settings className="w-8 h-8 text-gray-500" />
        <h1 className="text-2xl font-bold text-gray-900">设置</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Folder className="w-5 h-5 text-blue-500" />
              <CardTitle className="text-lg">文件存储设置</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                视频存储路径
              </label>
              <Input placeholder="./storage/videos" defaultValue="./storage/videos" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                封面存储路径
              </label>
              <Input placeholder="./storage/covers" defaultValue="./storage/covers" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                临时文件路径
              </label>
              <Input placeholder="./storage/temp" defaultValue="./storage/temp" />
            </div>
            <Button>保存设置</Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Database className="w-5 h-5 text-green-500" />
              <CardTitle className="text-lg">数据库设置</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="p-4 bg-gray-50 rounded-lg">
              <h4 className="font-medium text-gray-900 mb-2">当前数据库</h4>
              <p className="text-sm text-gray-600">H2 Database (开发环境)</p>
              <p className="text-xs text-gray-400 mt-1">
                路径: ./data/video_db
              </p>
            </div>
            <div className="flex gap-2">
              <Button variant="outline">导出数据</Button>
              <Button variant="outline">导入数据</Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Info className="w-5 h-5 text-purple-500" />
              <CardTitle className="text-lg">关于</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-500">应用名称</span>
              <span className="font-medium">本地视频播放管理平台</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">版本号</span>
              <span className="font-medium">1.0.0</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">前端框架</span>
              <span className="font-medium">React 18 + Vite</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">后端框架</span>
              <span className="font-medium">Spring Boot 3.2</span>
            </div>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-lg">支持的视频格式</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-2">
              {['MP4', 'AVI', 'MKV', 'MOV', 'FLV', 'WMV', 'WebM', 'M4V'].map((format) => (
                <span
                  key={format}
                  className="px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm font-medium"
                >
                  .{format.toLowerCase()}
                </span>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
