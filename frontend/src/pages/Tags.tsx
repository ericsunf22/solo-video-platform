import { useState } from 'react'
import { Tag, Plus, Edit2, Trash2, X, Check } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import type { Tag as TagType } from '@/types'

export default function Tags() {
  const [tags, setTags] = useState<TagType[]>([
    { id: 1, name: 'React', color: '#3b82f6', description: 'React 相关教程', createdAt: '2024-01-01T00:00:00Z', updatedAt: '2024-01-01T00:00:00Z', videoCount: 5 },
    { id: 2, name: 'TypeScript', color: '#3178c6', description: 'TypeScript 学习资料', createdAt: '2024-01-02T00:00:00Z', updatedAt: '2024-01-02T00:00:00Z', videoCount: 3 },
    { id: 3, name: 'Node.js', color: '#339933', description: 'Node.js 后端开发', createdAt: '2024-01-03T00:00:00Z', updatedAt: '2024-01-03T00:00:00Z', videoCount: 2 },
    { id: 4, name: '前端', color: '#e11d48', description: '前端开发相关', createdAt: '2024-01-04T00:00:00Z', updatedAt: '2024-01-04T00:00:00Z', videoCount: 8 },
  ])
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [newTagName, setNewTagName] = useState('')
  const [newTagColor, setNewTagColor] = useState('#3b82f6')
  const [newTagDescription, setNewTagDescription] = useState('')

  const colorOptions = ['#3b82f6', '#3178c6', '#339933', '#e11d48', '#f59e0b', '#8b5cf6', '#ec4899']

  const handleCreateTag = () => {
    if (!newTagName.trim()) return
    const newTag: TagType = {
      id: Date.now(),
      name: newTagName.trim(),
      color: newTagColor,
      description: newTagDescription.trim() || null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      videoCount: 0,
    }
    setTags([...tags, newTag])
    setNewTagName('')
    setNewTagColor('#3b82f6')
    setNewTagDescription('')
    setShowCreateForm(false)
  }

  const handleDeleteTag = (id: number) => {
    setTags(tags.filter((t) => t.id !== id))
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Tag className="w-8 h-8 text-purple-500" />
          <h1 className="text-2xl font-bold text-gray-900">标签管理</h1>
          <span className="text-sm text-gray-500 bg-gray-100 px-2 py-1 rounded">
            {tags.length} 个标签
          </span>
        </div>
        <Button onClick={() => setShowCreateForm(true)}>
          <Plus className="w-4 h-4 mr-2" />
          新建标签
        </Button>
      </div>

      {showCreateForm && (
        <Card className="mb-6">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>新建标签</CardTitle>
              <Button variant="ghost" size="icon" onClick={() => setShowCreateForm(false)}>
                <X className="w-4 h-4" />
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">标签名称</label>
                <Input
                  placeholder="请输入标签名称"
                  value={newTagName}
                  onChange={(e) => setNewTagName(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">标签颜色</label>
                <div className="flex gap-2">
                  {colorOptions.map((color) => (
                    <button
                      key={color}
                      className={`w-8 h-8 rounded-full border-2 ${
                        newTagColor === color ? 'border-gray-900' : 'border-transparent'
                      }`}
                      style={{ backgroundColor: color }}
                      onClick={() => setNewTagColor(color)}
                    />
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">描述（可选）</label>
                <Input
                  placeholder="请输入标签描述"
                  value={newTagDescription}
                  onChange={(e) => setNewTagDescription(e.target.value)}
                />
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="outline" onClick={() => setShowCreateForm(false)}>
                  取消
                </Button>
                <Button onClick={handleCreateTag}>
                  <Check className="w-4 h-4 mr-2" />
                  确认创建
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {tags.length === 0 ? (
        <div className="text-center py-16">
          <Tag className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">暂无标签</h3>
          <p className="text-gray-500">创建标签来分类管理你的视频</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {tags.map((tag) => (
            <Card key={tag.id} className="hover:shadow-md transition-shadow">
              <CardContent className="p-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div
                      className="w-10 h-10 rounded-full flex items-center justify-center"
                      style={{ backgroundColor: tag.color + '20' }}
                    >
                      <Tag className="w-5 h-5" style={{ color: tag.color }} />
                    </div>
                    <div>
                      <h3 className="font-medium text-gray-900">{tag.name}</h3>
                      {tag.description && (
                        <p className="text-sm text-gray-500 mt-1">{tag.description}</p>
                      )}
                    </div>
                  </div>
                  <div className="flex gap-1">
                    <Button variant="ghost" size="icon" className="h-8 w-8">
                      <Edit2 className="w-4 h-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-red-500 hover:text-red-600"
                      onClick={() => handleDeleteTag(tag.id)}
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
                <div className="mt-3 pt-3 border-t">
                  <span className="text-sm text-gray-500">
                    {tag.videoCount} 个视频
                  </span>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
